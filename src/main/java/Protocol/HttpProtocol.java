package Protocol;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

public class HttpProtocol extends Protocol{
    private static final String TYPE = "http";

    public static Map<String, List<String>> initHeaders() {
        return new LinkedHashMap<>();
    }

    @Override
    public String getType() {
        return TYPE;
    }
    public HttpProtocol(){

    }

    @Override
    public HttpRequest decoder(byte[] data) {
        //修改：先定位头部结束符，正文保留为原始字节
        int end = 0;
        while (end + 3 < data.length) {
            if (data[end] == '\r' && data[end + 1] == '\n'
                    && data[end + 2] == '\r' && data[end + 3] == '\n') {
                break;
            }
            end++;
        }
        if (end + 3 >= data.length) return null;

        String[] lines = new String(data, 0, end, StandardCharsets.UTF_8).split("\r\n");
        String[] requestLine = lines[0].trim().split("\\s+");
        if (requestLine.length != 3) throw new IllegalArgumentException("无效的请求行");

        Map<String, List<String>> headers = initHeaders();

        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon <= 0) throw new IllegalArgumentException("无效的请求头");
            String key = lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = lines[i].substring(colon + 1).trim();
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        int length = 0;
        List<String> lengths = headers.get("content-length");
        if (headers.containsKey("transfer-encoding")) {
            throw new IllegalArgumentException("当前只支持固定长度请求");
        }
        if (lengths != null) {
            if (lengths.size() != 1 || !lengths.get(0).matches("[0-9]+")) {
                throw new IllegalArgumentException("无效的 Content-Length");
            }
            length = Integer.parseInt(lengths.get(0));
        }
        int bodyStart = end + 4;
        if (data.length - bodyStart < length) return null;
        byte[] body = Arrays.copyOfRange(data, bodyStart, bodyStart + length);
        return new HttpRequest(requestLine[0], requestLine[1], requestLine[2], headers, body);
    }

    @Override
    public byte[] encoder(Object message) {
        HttpResponse httpResponse = (HttpResponse)message;
        StringBuilder sb = new StringBuilder();
        sb.append(httpResponse.getVersion());
        sb.append(' ');
        sb.append(httpResponse.getStatueCode());
        sb.append(' ');
        sb.append(httpResponse.getMsg());
        sb.append("\r\n");

        byte[] body = httpResponse.getBody();
        int bodyLength = body == null ? 0 : body.length;
        httpResponse.getMap().put("Content-Length",List.of(String.valueOf(bodyLength)));
        for(Map.Entry<String,List<String>> entry : httpResponse.getMap().entrySet()){
            if(entry.getValue()!=null) {
                String key = entry.getKey();
                for(String value : entry.getValue()) {
                    sb.append(key);
                    sb.append(": ");
                    sb.append(value);
                    sb.append("\r\n");
                }
            }
        }
        sb.append("\r\n");
        byte[] headers = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[headers.length + bodyLength];
        System.arraycopy(headers,0,result,0,headers.length);
        if(bodyLength > 0){
            System.arraycopy(body,0,result,headers.length,bodyLength);
        }
        return result;
    }


    @Override
    boolean isLegal() {
        return false;
    }
   public boolean isArrived(String s) {
        int hasArrived = s.indexOf("\r\n\r\n");
        if (hasArrived < 0) {
            return false;
        }

        int headLength = hasArrived + 4;
        int contentLength = 0;

        String content = "Content-Length:";
        int pos = s.indexOf(content);

        if (pos >= 0 && pos < hasArrived) {
            int begin = pos + content.length();
            int end = s.indexOf("\r\n", begin);
            //get body length
            contentLength = Integer.parseInt(s.substring(begin, end).trim());
        }
        return s.length() >= (long) headLength + contentLength;
    }

}
