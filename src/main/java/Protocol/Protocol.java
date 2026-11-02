package Protocol;

public abstract class Protocol {

    public abstract String getType();
    public abstract Object decoder(byte[] data);
    public abstract byte[] encoder(Object message);
    abstract boolean isLegal();
}
