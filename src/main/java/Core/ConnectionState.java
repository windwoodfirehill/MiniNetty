package Core;

import java.nio.ByteBuffer;

class ConnectionState {
    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    ByteBuffer writeBuffer;
}
