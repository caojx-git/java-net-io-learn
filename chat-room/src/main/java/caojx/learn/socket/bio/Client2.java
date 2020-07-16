package caojx.learn.socket.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * BIO socket编程 运行改进的客户端实例
 * <p>
 * 改进的效果：
 * 1、客户端可以一直发送数据，如果发送的是 quit 的数据，那么服务器就关闭，客户端这个时候也需要关闭
 *
 * @author caojx created on 2020/6/13 3:12 下午
 */
public class Client2 {

    public static void main(String[] args) {
        final String QUIT = "quit";
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_PORT = 8888;
        Socket socket = null;
        BufferedWriter writer = null;

        try {
            // 创建socket
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_PORT);

            // 创建IO流
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // 等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String input = consoleReader.readLine();

                // 发送消息给服务器
                writer.write(input + "\n");
                writer.flush();

                // 读取服务器返回的消息
                String msg = reader.readLine();
                System.out.println("" + msg);

                // 查看用户是否退出
                if(QUIT.equals(input)){
                    break;
                }
            }

        } catch (Exception e) {

        } finally {
            if (null != writer) {
                try {
                    writer.close();
                    System.out.println("关闭Socket");
                } catch (Exception e) {

                }
            }
        }

    }
}