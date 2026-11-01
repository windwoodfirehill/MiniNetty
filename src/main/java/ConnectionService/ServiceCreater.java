package ConnectionService;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServiceCreater {
    private final MyChannel myChannel;

    public ServiceCreater(SocketAddress socketAddress) throws IOException {
        myChannel = new MyChannel(ServerSocketChannel.open());
        myChannel.setNoBlocking();
        myChannel.bind(socketAddress);
    }

    public void register(Selector selector) throws ClosedChannelException {
        myChannel.register(selector);
    }

    public SocketChannel acceptConnection() throws IOException {
        SocketChannel socketChannel = myChannel.accept();
        if (socketChannel == null) {
            return null;
        }
        return socketChannel;
    }

}
