package IOtest;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class MyJDKChannel{
    private final ServerSocketChannel serverSocketChannel;
    public MyJDKChannel(ServerSocketChannel serverSocketChannel){
        this.serverSocketChannel = serverSocketChannel;
    }
    public void bind(SocketAddress address) throws IOException {
        serverSocketChannel.bind(address);
    }
    public SocketChannel accept() throws IOException {
        return serverSocketChannel.accept();
    }
    public void setNoBlocking() throws IOException {
        serverSocketChannel.configureBlocking(false);
    }

    public void register(Selector selector) throws ClosedChannelException {
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
}