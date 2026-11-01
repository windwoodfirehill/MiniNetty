package ConnectionService;

import IOtest.MyJDKChannel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;

public class MyChannel {
    private final ServerSocketChannel serverSocketChannel;
    public MyChannel(ServerSocketChannel serverSocketChannel){
        this.serverSocketChannel = serverSocketChannel;
    }
    //绑定地址
    public void bind(SocketAddress socketAddress) throws IOException {
        serverSocketChannel.bind(socketAddress);
    }
    //接收链接
    public SocketChannel accept() throws IOException {
        return serverSocketChannel.accept();
    }
    //设置为非阻塞
    public void setNoBlocking() throws IOException {
        serverSocketChannel.configureBlocking(false);
    }

    public void register(Selector selector) throws ClosedChannelException {
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

}
