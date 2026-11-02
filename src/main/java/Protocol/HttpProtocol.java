package Protocol;

public class HttpProtocol extends Protocol{
    private static final String TYPE = "http";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Object decoder(byte[] data) {
        return null;
    }

    @Override
    public byte[] encoder(Object message) {
        return new byte[0];
    }

    @Override
    boolean isLegal() {
        return false;
    }
}
