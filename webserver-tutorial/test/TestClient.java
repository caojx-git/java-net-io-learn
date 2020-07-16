import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 测试客户端，发送网络请求（模拟实现postman那种工具）
 */
public class TestClient {

    public static void main(String[] args) throws Exception {
        // 创建socket连接服务器
        Socket socket = new Socket("localhost", 8888);

        // 发送请求
        OutputStream output = socket.getOutputStream();
//        output.write("GET /index.html  HTTP/1.1".getBytes());
        output.write("GET /servlet/TimeServlet HTTP/1.1".getBytes());
        socket.shutdownOutput();

        // 读取服务器响应消息
        InputStream input = socket.getInputStream();
        byte[] buffer = new byte[2048];
        int length = input.read(buffer);
        StringBuilder response = new StringBuilder();
        for (int j = 0; j < length; j++) {
            response.append((char) buffer[j]);
        }
        System.out.println(response.toString());
        socket.shutdownInput();

        // 关闭客户端
        socket.close();
    }
}
