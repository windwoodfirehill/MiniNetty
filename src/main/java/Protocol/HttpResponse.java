package Protocol;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {
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
}
