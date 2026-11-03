package Protocol;

public class HttpProtocol extends Protocol{
    private static final String TYPE = "http";
    private int contentLength = -1;

    private int totalLength;

    private int headLength;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Object decoder(byte[] data) {
        String s = new String(data);
        return new byte[1];
    }

    @Override
    public byte[] encoder(Object message) {
        return new byte[0];
    }

    @Override
    boolean isLegal() {
        return false;
    }
    public void setContentLength(int len) {
        this.contentLength = len;
    }

    public void setHeadLength(int len) {
        this.headLength = len;
    }

    public int getHeadLength() {
        return this.headLength;
    }

    public int getContentLength() {
        return this.contentLength;
    }

   public boolean isArrived(String s) {
        int hasArrived = s.indexOf("\r\n\r\n");
        if (hasArrived < 0) {
            return false;
        }

        setHeadLength(hasArrived + 4);

        setContentLength(0);

        String content = "Content-Length:";
        int pos = s.indexOf(content);

        if (pos >= 0 && pos < hasArrived) {
            int begin = pos + content.length();
            int end = s.indexOf("\r\n", begin);
            //get body length
            int len = Integer.parseInt(s.substring(begin, end).trim());

            setContentLength(len);
        }
        return s.length() >= (long) getHeadLength() + getContentLength();
    }
}
