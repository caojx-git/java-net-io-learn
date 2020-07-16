package caojx.learn.socket.bio;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO socket编程 运行简单的服务器客户端实例
 * <p>
 * 服务器端收到了 hello 的消息，然后客户端{@link Client} 发送完消息后就关闭了自己的 socket。
 *
 * @author caojx created on 2020/6/10 4:31 下午
 */
public class Server {

    public static void main(String[] args) {
        final int DEFAULT_PORT = 8888;
        ServerSocket serverSocket = null;

        try {
            // 绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口" + DEFAULT_PORT);
            while (true) {
                // 等待客户端连接
                Socket socket = serverSocket.accept();
                System.out.println("客户端[" + socket.getPort() + "]已连接");

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // 读取客户端发送的消息，这个时候只能读一行消息，后面改善
                String msg = reader.readLine();
                if (null != msg) {
                    System.out.println("客户端[" + socket.getPort() + "]:" + msg);
                    // 回复客户端发送的消息
                    writer.write("服务器:" + msg + "\n");
                    writer.flush(); // 缓冲区的数据发送出去
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != serverSocket) {
                try {
                    serverSocket.close();
                    System.out.println("关闭 ServerSocket");
                } catch (Exception e) {

                }
            }
        }

    }
}