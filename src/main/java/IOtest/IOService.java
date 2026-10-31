package IOtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class IOService {
    private final MyJDKChannel mj;

    public IOService(SocketAddress socketAddress) throws IOException {
        this.mj = new MyJDKChannel(ServerSocketChannel.open());

        // 注册 Selector 前必须设置非阻塞
        mj.setNoBlocking();

        // 绑定地址和端口
        mj.bind(socketAddress);
    }

    // 让服务端 Channel 注册到 Selector
    public void register(Selector selector) throws IOException {
        mj.register(selector);
    }

    public SocketChannel acceptConnection() throws IOException {
        SocketChannel socketChannel = mj.accept();

        // 非阻塞模式下，没有连接可接受时可能返回 null
        if (socketChannel == null) {
            return null;
        }

        System.out.println("客户端已连接：" + socketChannel.getRemoteAddress());
        return socketChannel;
    }
}

class Main {
    private static final int PORT = 8888;
    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        IOService ioService = new IOService(new InetSocketAddress(HOST, PORT));

        // 1. 创建 Selector
        Selector selector = Selector.open();

        // 2. 注册服务端 Channel，关注 OP_ACCEPT
        ioService.register(selector);

        System.out.println("服务端已启动：" + HOST + ":" + PORT);

        while (true) {
            // 3. 等待事件，返回就绪 Channel 的数量
            int readyCount = selector.select();

            if (readyCount == 0) {
                continue;
            }

            // 4. 获取就绪事件集合
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                // 5. 从集合移除当前事件，避免下一轮重复处理
                iterator.remove();


                // 6. 服务端 Channel 有连接可接受
                if (key.isAcceptable()) {
                    SocketChannel socketChannel = ioService.acceptConnection();
                    if (socketChannel == null) {
                        continue;
                    }
                    socketChannel.configureBlocking(false);
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    socketChannel.register(selector,SelectionKey.OP_READ,byteBuffer);
                }
                //如果可读
                if(key.isReadable()){
                    //先取出channel
                    SocketChannel client = (SocketChannel)key.channel();
                    //取出buffer
                    ByteBuffer byteBuffer = (ByteBuffer)key.attachment();
                    //先要获取buffer的大小
                    int count;
                    try {
                        count = client.read(byteBuffer);
                    } catch (IOException e) {
                        // 一个客户端异常断开时，只关闭该连接，不退出事件循环
                        key.cancel();
                        client.close();
                        continue;
                    }
                    if(count > 0) {
                        //前置
                        byteBuffer.flip();
                        //开启缓冲区
                        byte[] data = new byte[count];
                        //读到jvm里
                        byteBuffer.get(data);
                        System.out.println(new String(data));
                        byteBuffer.clear();
                    }if(count == -1){
                        key.cancel();
                        client.close();
                    }
                }
            }
        }
    }
}
