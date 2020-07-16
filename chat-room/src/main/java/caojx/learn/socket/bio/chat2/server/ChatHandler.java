package caojx.learn.socket.bio.chat2.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * 实现监听器
 * <p>
 * 需要 ChatServer 的对象：很多的操作都是依赖于 ChatServer 的connectedClients对象
 * 需要客户端的 Socket 对象
 * 前面的ChatServer 代码中添加readToQuit的方法代码
 * 最后需要注意线程安全问题，需要注意一下
 *
 * @author caojx created on 2020/6/13 6:50 下午
 */
public class ChatHandler implements Runnable {

    private ChatServer chatServer;
    private Socket socket;

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 存储新上线的用户
            chatServer.addClient(socket);

            // 读取用户发送的消息
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // readLine是一直阻塞的，除非收到换行就会读取
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                String fwdMsg = "客户端[" + socket.getPort() + "]:" + msg + "\n";
                System.out.println(fwdMsg);
                // 将消息转发给聊天室在线的其他用户
                chatServer.forwardMessage(socket, fwdMsg);
                // 检查是否准备退出
                if (chatServer.readToQuit(msg)) {
                    break; // 如果收到退出命令，那么就直接退出
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                chatServer.removeClient(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}