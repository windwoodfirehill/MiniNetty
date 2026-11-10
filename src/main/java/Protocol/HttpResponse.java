package Protocol;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse implements Protocol {
    //状态行
    //版本
    private String version;
    //状态码
    private int statueCode;
    //状态信息
    private String msg;
    //响应头
    private Map<String, List<String>> responseHeaders;

    private byte[] body = null;

    public  HttpResponse(){
        responseHeaders = new LinkedHashMap<>();
    }
    //新增：创建可直接交给 encoder 的响应
    public HttpResponse(String version, int statueCode, String msg, byte[] body) {
        this();
        this.version = version;
        this.statueCode = statueCode;
        this.msg = msg;
        this.body = body;
    }

    public String getVersion(){
        return  this.version;
    }
    public int getStatueCode(){
        return  this.statueCode;
    }
    public String getMsg(){
        return this.msg;
    }

    public Map<String,List<String>> getMap(){
        return  this.responseHeaders;
    }
    public byte[] getBody(){
        return this.body;
    }

    @Override
    public int getLength() {
        int bodyLength = body == null ? 0 : body.length;
        int length = utf8Length(version + " " + statueCode + " " + msg + "\r\n");
        length += utf8Length("Content-Length: " + bodyLength + "\r\n");
        for (Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
            if ("Content-Length".equals(header.getKey()) || header.getValue() == null) {
                continue;
            }
            for (String value : header.getValue()) {
                length += utf8Length(header.getKey() + ": " + value + "\r\n");
            }
        }
        return length + 2 + bodyLength; // 响应头和正文之间的空行占 2 字节
    }

    private static int utf8Length(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
