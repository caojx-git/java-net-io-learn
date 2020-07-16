package caojx.learn.socket.bio.chat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于BIO模型实现的多人聊天室设计-服务端
 * <p>
 * 不停的监听和接收客户端的消息
 * 收到客户端消息后转发给在线的所有的客户端
 * 需要保存所有的在线客户端列表
 * 当用户输入 quit 的时候，这个客户就不想留在服务器端，就需要将它从在线用户列表中移除
 *
 * @author caojx created on 2020/6/13 6:33 下午
 */
public class ChatServer {

    private final int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";

    private ServerSocket serverSocket;

    /**
     * 连接的客户端列表
     * key是客户端标识（暂时用端口指代）
     * value 是需要写消息的 writer
     */
    private Map<Integer, Writer> connectedClients;

    public ChatServer() {
        connectedClients = new HashMap<>();
    }

    /**
     * 添加客户端
     *
     * @param socket
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (null != socket) {
            int port = socket.getPort();
            OutputStream out = socket.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            connectedClients.put(port, writer); // 线程安全问题，使用synchronized关键字解决
            System.out.println("客户端[" + port + "]已经连接到服务器");
        }
    }

    /**
     * 移除客户端，同时关闭客户端
     *
     * @param socket
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (null != socket) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                Writer writer = connectedClients.get(port);
                writer.close();
            }
            connectedClients.remove(port);
            System.out.println("客户端[" + port + "已经断开连接");
        }
    }

    /**
     * 转发消息给客户端
     *
     * @param socket
     * @param fwdMsg
     */
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Map.Entry<Integer, Writer> entry : connectedClients.entrySet()) {
            int port = entry.getKey();

            // 转发消息给其他客户端
            if (socket.getPort() != port) {
                Writer writer = entry.getValue();
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    /**
     * 检查是否退出
     *
     * @param msg
     * @return
     */
    public boolean readToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 关闭serverSocket
     */
    public synchronized void close() {
        if (null != serverSocket) {
            try {
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动
     */
    public void start() {
        try {
            // 绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口" + DEFAULT_PORT + "...");

            while (true) {
                // 等待客户端连接
                Socket socket = serverSocket.accept();
                // 创建 ChatHandler线程
                new Thread(new ChatHandler(this, socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}