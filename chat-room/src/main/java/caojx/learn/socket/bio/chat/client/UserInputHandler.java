package caojx.learn.socket.bio.chat.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 实现用户输入监听器
 *
 * @author caojx created on 2020/6/13 10:48 下午
 */
public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        // 等待用户输入信息
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String input = consoleReader.readLine();
                // 向服务器发送消息
                chatClient.send(input);
                // 检查用户是否退出
                if (chatClient.readyToQuit(input)) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}