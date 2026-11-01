package Core;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

//新增：直接运行 main；客户端、断言、服务端子进程管理全部由 Java 完成。
public class MiniNettyIntegrationTest {
    public static void main(String[] args) throws Exception {
        testDispatcher();
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) {
            port = reservation.getLocalPort();
        }
        Process server = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                "MiniNettyApplication.Main", Integer.toString(port))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            awaitReady(port, server);
            try (Socket client = connect(port)) {
                for (int i = 0; i < 20; i++) {
                    echo(client, ("hello-中文-" + i).getBytes(StandardCharsets.UTF_8));
                }
                byte[] large = new byte[256 * 1024];
                Arrays.fill(large, (byte) 'x');
                echo(client, large);
            }
            System.out.println("PASS repeated echo, UTF-8 bytes, 256 KiB payload");

            List<Socket> clients = new ArrayList<>();
            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                //先确认每条连接已交接，单独验证已有连接的并发读写。
                for (int i = 0; i < 8; i++) {
                    Socket client = connect(port);
                    clients.add(client);
                    echo(client, new byte[]{42});
                }
                List<Future<?>> tasks = new ArrayList<>();
                for (int i = 0; i < clients.size(); i++) {
                    final int id = i;
                    Socket client = clients.get(i);
                    tasks.add(pool.submit(() -> {
                        for (int round = 0; round < 30; round++) {
                            byte[] payload = ("client=" + id + ",round=" + round)
                                    .getBytes(StandardCharsets.UTF_8);
                            try { echo(client, payload); }
                            catch (IOException e) { throw new UncheckedIOException(e); }
                        }
                    }));
                }
                for (Future<?> task : tasks) task.get(20, TimeUnit.SECONDS);
            } finally {
                for (Socket client : clients) client.close();
                pool.shutdownNow();
            }
            System.out.println("PASS 8 established clients, 240 concurrent echo exchanges");
            try (Socket reset = connect(port)) {
                echo(reset, new byte[]{1, 2, 3});
                reset.setSoLinger(true, 0);
            }
            for (int i = 0; i < 10; i++) {
                try (Socket client = connect(port)) { echo(client, new byte[]{4, 5, 6}); }
            }
            System.out.println("PASS abrupt disconnect followed by 10 reconnects");
            if (!server.isAlive()) throw new AssertionError("Server exited unexpectedly");
        } finally {
            //当前 shutdown 尚不关闭资源，使用独立 JVM，退出后由 OS 回收测试资源。
            server.destroy();
            if (!server.waitFor(5, TimeUnit.SECONDS)) {
                server.destroyForcibly();
                server.waitFor(5, TimeUnit.SECONDS);
            }
        }
        System.out.println("ALL TESTS PASSED");
    }

    private static void testDispatcher() throws Exception {
        WorkerLoop first = new WorkerLoop();
        WorkerLoop second = new WorkerLoop();
        List<SocketChannel> pending = new ArrayList<>(); //新增：填满扩大后的队列
        try (SocketChannel a = SocketChannel.open();
             SocketChannel b = SocketChannel.open();
             SocketChannel c = SocketChannel.open()) {
            ChannelDispatcher dispatcher = new ChannelDispatcher();
            check(!dispatcher.submit(a), "Empty dispatcher must reject");
            dispatcher.injectWorkerLoop(first);
            dispatcher.injectWorkerLoop(second);
            check(dispatcher.submit(a) && dispatcher.submit(b), "Two workers must accept");
            check(first.queue.peek() == a && second.queue.peek() == b, "Round-robin assignment");
            //新增：两个 Worker 都不启动，确定性验证待注册队列的容量和边界。
            for (int i = 0; i < 2 * (WorkerLoop.QUE_SIZE - 1); i++) {
                SocketChannel channel = SocketChannel.open();
                pending.add(channel);
                check(dispatcher.submit(channel), "Queue rejected before reaching capacity");
            }
            check(first.queue.size() == 1024 && second.queue.size() == 1024,
                    "Each worker should hold 1024 pending connections");
            check(!dispatcher.submit(c), "Full queue must reject");
            first.queue.poll();
            //下一次选中 second：它仍满，而 first 已空。记录当前策略的限制。
            check(!dispatcher.submit(c), "Current policy does not retry another worker");
            System.out.println("PASS dispatcher round-robin, empty/full rejection");
            System.out.println("LIMIT: selected worker full => reject, even if another worker is free");
        } finally {
            for (SocketChannel channel : pending) channel.close(); //新增：释放测试通道
            first.selector.close();
            second.selector.close();
        }
    }

    private static Socket connect(int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
            socket.setSoTimeout(5000);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    private static void awaitReady(int port, Process server) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (!server.isAlive()) throw new AssertionError("Server failed to start");
            try (Socket socket = connect(port)) {
                echo(socket, new byte[]{7});
                return;
            } catch (ConnectException e) { Thread.sleep(25); }
        }
        throw new AssertionError("Server startup timed out");
    }

    private static void echo(Socket client, byte[] expected) throws IOException {
        client.getOutputStream().write(expected);
        byte[] actual = client.getInputStream().readNBytes(expected.length);
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Echo mismatch: expected " + expected.length + ", got " + actual.length);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
