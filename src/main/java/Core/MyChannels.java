package Core;

import java.io.IOException;
import java.nio.channels.Channel;

public class MyChannels implements Channel {

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
