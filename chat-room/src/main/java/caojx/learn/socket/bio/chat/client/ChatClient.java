package caojx.learn.socket.bio.chat.client;

import java.io.*;
import java.net.Socket;

/**
 * 基于BIO模型实现的多人聊天室设计-客户端
 * <p>
 * 先完成主线程：将消息发送给服务器，接收服务器返回的消息
 *
 * @author caojx created on 2020/6/13 6:57 下午
 */
public class ChatClient {

    private String DEFAULT_SERVER_HOST = "127.0.0.1";
    private int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    /**
     * 发送消息给服务器
     *
     * @param msg
     * @throws IOException
     */
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    /**
     * 从服务器接收消息
     *
     * @return
     * @throws
     */
    public String receive() {
        String msg = null;
        if (!socket.isInputShutdown()) {
            try {
                msg = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return msg;
    }

    /**
     * 检查是否退出
     *
     * @param msg
     * @return
     */
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 关闭writer
     */
    public synchronized void close() {
        if (null != writer) {
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            // 创建 Socket
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);

            // 创建IO流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // 处理用户的输入（需要额外开线程来进行处理）
            new Thread(new UserInputHandler(this)).start();

            // 时刻监听是否有从服务器其他用户发送过来消息
            String msg = null;
            while ((msg = receive()) != null) {
                System.out.println(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }

}