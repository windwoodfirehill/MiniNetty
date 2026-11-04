package Protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpProtocol extends Protocol{
    private static final String TYPE = "http";
    private int contentLength = -1;

    private int totalLength;

    private int headLength;

    private boolean lazyMode = true;

    public static Map<String, List<String>> initHeaders() {
        Map<String, List<String>> map = new HashMap<>();
        String[] keys = {
                "Host", "User-Agent", "Connection", "Keep-Alive", "Date",
                "Cache-Control", "Pragma", "Via", "Warning",
                "Accept", "Accept-Language", "Accept-Encoding", "Accept-Charset", "TE",
                "Content-Type", "Content-Length", "Content-Encoding", "Content-Language",
                "Transfer-Encoding", "Expect", "Trailer",
                "If-Match", "If-None-Match", "If-Modified-Since",
                "If-Unmodified-Since", "If-Range", "Range",
                "Authorization", "Proxy-Authorization", "Cookie",
                "Referer", "Origin", "Upgrade", "Upgrade-Insecure-Requests",
                "Sec-Fetch-Dest", "Sec-Fetch-Mode", "Sec-Fetch-Site", "Sec-Fetch-User",
                "Sec-Ch-Ua", "Sec-Ch-Ua-Mobile", "Sec-Ch-Ua-Platform", "DNT",
                "X-Forwarded-For", "X-Real-IP"
        };
        for (String key : keys) {
            map.put(key, new ArrayList<>());
        }
        return map;
    }

    @Override
    public String getType() {
        return TYPE;
    }
    public HttpProtocol(){

    }

    @Override
    public Object decoder(byte[] data) {
        String http = new String(data);
        String[] lines = http.trim().split("\r\n");
        //按照空格切分
        String[] requestLine = lines[0].trim().split("\\s+");
        String method = requestLine[0];
        String target = requestLine[1];
        String version = requestLine[2];
        HttpRequest httpRequest = new HttpRequest(method,target,version,initHeaders(),null);

        Map<String,List<String>> headers = httpRequest.getHeaders();
        for(int i = 1; i < lines.length ; i ++){
            int idx = lines[i].indexOf(":");
            if(idx <= 0){continue;}

            String key = lines[i].substring(0,idx).trim();
            String value = lines[i].substring(idx+1).trim();

            if(headers.containsKey(key)){
                headers.get(key).add(value);
            }
        }

        String body = null;
        HttpRequest finalRequest;
        if(lazyMode == true) {
            List<String> contents = headers.get("Content-Length");
            if (contents != null && !contents.isEmpty()) {
                int len = Integer.parseInt(contents.get(0).trim());
                if (len > 0) {
                    int idx = http.indexOf("\r\n\r\n");
                    body = idx >= 0 ? http.substring(idx + 4).trim() : "";
                    if (body.length() > len) {
                        body.substring(0, len);
                        finalRequest = new HttpRequest(method,target,version,headers,body.getBytes());
                    }
                }
            }
        }else{
            if(contentLength > 0){
                int idx = http.indexOf("\r\n\r\n");
                body = idx >= 0 ? http.substring(idx + 4).trim() : "";
                if (body.length() > contentLength) {
                    body.substring(0, contentLength);
                    finalRequest = new HttpRequest(method,target,version,headers,body.getBytes());
                }
            }
        }
        System.out.println(method+" "+target+" "+version+" ");
        for(Map.Entry<String,List<String>> entry : headers.entrySet()){
            if(!entry.getValue().isEmpty()){
                System.out.println(entry.getKey() + ":" + entry.getValue().get(0));
            }
        }
        return new Object();

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
