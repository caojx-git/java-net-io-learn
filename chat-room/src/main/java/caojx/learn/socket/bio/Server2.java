package caojx.learn.socket.bio;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO socket编程 运行改进的服端实例
 * <p>
 * 改进的效果：
 * 1、客户端可以一直发送数据，如果发送的是 quit 的数据，那么服务器就关闭，客户端这个时候也需要关闭
 *
 * @author caojx created on 2020/6/10 4:31 下午
 */
public class Server2 {

    public static void main(String[] args) {
        final String QUIT = "quit";
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
                String msg = null;
                while((msg = reader.readLine()) != null) {
                    System.out.println("客户端[" + socket.getPort() + "]:" + msg);
                    // 回复客户端发送的消息
                    writer.write("服务器:" + msg + "\n");
                    writer.flush(); // 缓冲区的数据发送出去

                    // 查看客户端是否退出，其实这里的代码不用写，因为客户端的Socket关闭的话，那么readerLine的返回的内容是null，也就不会进入这个循环了
                    if (QUIT.equals(msg)) {
                        System.out.println("客户端[" + socket.getPort() + "]已断开");
                        break;
                    }
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