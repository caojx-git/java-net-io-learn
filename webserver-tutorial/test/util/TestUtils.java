package util;

import connector.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtils {

    /**
     * 创建request对象
     *
     * @param requestStr 比如，GET /index.html HTTP/1.1
     * @return
     */
    public static Request createRequest(String requestStr) {
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());
        Request request = new Request(input);
        request.parse();
        return request;
    }

    /**
     * 读取文件内容
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static String readFileToString(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
}
