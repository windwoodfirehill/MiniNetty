package Protocol;

public abstract class Protocol {

    //新增：按类型创建解析器，每条连接调用一次


    public abstract String getType();
    public abstract Object decoder(byte[] data);
    public abstract byte[]   encoder(Object message);
    abstract boolean isLegal();
    public abstract int isArrived(byte[] data);
}
