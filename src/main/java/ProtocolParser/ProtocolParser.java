package ProtocolParser;

import Protocol.Protocol;

public interface ProtocolParser {
    String getType();
    Protocol decoder(byte[] data);
    byte[] encoder(Protocol message);
    boolean isLegal();
    int isArrived(byte[] data);
}
