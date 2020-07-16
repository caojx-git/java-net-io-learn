package connector;

import java.io.File;

public class ConnectorUtils {

    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";
//    public static final String WEB_ROOT = ConnectorUtils.class.getClassLoader().getResource("webroot").getPath();

    public static final String PROTOCOL = "HTTP/1.1";

    public static final String CARRIAGE = "\r";

    public static final String NEWLINE = "\n";

    public static final String SPACE = " ";

    /**
     * 响应状态行，比如 HTTP/1.1 200 OK
     *
     * @param status 状态码
     * @return
     */
    public static String renderStatus(HttpStatus status) {
        StringBuilder sb = new StringBuilder(PROTOCOL)
                .append(SPACE)
                .append(status.getStatusCode())
                .append(SPACE)
                .append(status.getReason())
                .append(CARRIAGE).append(NEWLINE)
                .append(CARRIAGE).append(NEWLINE);

        return sb.toString();
    }

}
