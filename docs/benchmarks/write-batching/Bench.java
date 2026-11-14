import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class Bench {
    static final int MAX = 4096;
    record Result(long nanos, int writes, int sent, int rejected) {}
    static Result run(boolean batch, byte[][] messages) throws Exception {
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        int accepted = 0;
        while (accepted < messages.length && messages[accepted].length <= MAX)
            expected.writeBytes(messages[accepted++]);
        byte[] bytes = expected.toByteArray();
        try (ServerSocketChannel server = ServerSocketChannel.open();
             SocketChannel sender = SocketChannel.open();
             Selector selector = Selector.open()) {
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            sender.connect(server.getLocalAddress());
            sender.setOption(StandardSocketOptions.TCP_NODELAY, true);
            sender.configureBlocking(false);
            try (SocketChannel receiver = server.accept()) {
                FutureTask<byte[]> read = new FutureTask<>(() ->
                    receiver.socket().getInputStream().readAllBytes());
                new Thread(read).start();
                SelectionKey key = sender.register(selector, 0);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                buffer.flip();
                int next = 0, writes = 0;
                long start = System.nanoTime();
                while (buffer.hasRemaining() || next < messages.length) {
                    if (batch || !buffer.hasRemaining()) {
                        buffer.compact();
                        while (next < messages.length && messages[next].length <= MAX) {
                            byte[] data = messages[next];
                            int required = buffer.position() + data.length;
                            if (required > MAX) break;
                            if (required > buffer.capacity()) {
                                int capacity = buffer.capacity();
                                while (capacity < required) capacity *= 2;
                                ByteBuffer larger = ByteBuffer.allocate(capacity);
                                buffer.flip();
                                larger.put(buffer);
                                buffer = larger;
                            }
                            buffer.put(data);
                            next++;
                            if (!batch) break;
                        }
                        buffer.flip();
                    }
                    if (!buffer.hasRemaining()) break;
                    writes++;
                    if (sender.write(buffer) == 0) {
                        key.interestOps(SelectionKey.OP_WRITE);
                        if (selector.select(5000) == 0) throw new IOException("write timed out");
                        selector.selectedKeys().clear();
                        key.interestOps(0);
                    }
                }
                sender.shutdownOutput();
                byte[] actual = read.get(10, TimeUnit.SECONDS);
                long elapsed = System.nanoTime() - start;
                if (!Arrays.equals(bytes, actual) || next != accepted)
                    throw new AssertionError("byte stream or FIFO mismatch");
                return new Result(elapsed, writes, next, messages.length - next);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        boolean batch = args[0].equals("batch");
        if (args[1].equals("boundary")) {
            for (int size : new int[]{4095, 4096, 4097}) {
                Result r = run(batch, new byte[][]{new byte[]{11}, new byte[size], new byte[]{22}});
                if (r.sent() != (size <= MAX ? 3 : 1)) throw new AssertionError();
                System.out.printf("%s %d PASS sent=%d rejected=%d%n", args[0], size, r.sent(), r.rejected());
            }
            return;
        }
        int size = Integer.parseInt(args[1]);
        int count = 16000;
        byte[][] data = new byte[count][size];
        Random random = new Random(20261114);
        for (byte[] message : data) random.nextBytes(message);
        for (int i = 0; i < 2; i++) run(batch, data);
        for (int i = 0; i < 5; i++) {
            Result r = run(batch, data);
            System.out.printf(Locale.ROOT, "%s,%d,%d,%.3f,%d%n", args[0], size, count, r.nanos()/1e6, r.writes());
        }
    }
}
