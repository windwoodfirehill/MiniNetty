package Protocol;

import java.util.List;
import java.util.Map;

public class HttpRequest {

    // 请求方法：GET、POST 等
    private final String method;

    // 请求目标，例如 /users?id=1
    private final String target;

    // HTTP 版本，例如 HTTP/1.1
    private final String version;

    // 请求头：一个名称可能对应多个值
    private final Map<String, List<String>> headers;

    // 请求体：文本、图片等都是字节
    private final byte[] body;

    public HttpRequest(String method, String target, String version,
                       Map<String, List<String>> headers, byte[] body) {
        this.method = method;
        this.target = target;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getTarget() {
        return target;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}