package caojx.learn.socket.nio.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 使用nio编程模型实现多人聊天室-处理用户输入信息
 *
 * @author caojx created on 2020/6/26 6:55 下午
 */
public class UserInputHandler implements Runnable {

    private ChatClient chartClient;

    public UserInputHandler(ChatClient chartClient) {
        this.chartClient = chartClient;
    }

    @Override
    public void run() {
        // 等待用户输入消息
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String input = consoleReader.readLine();
                // 向服务器发送消息
                chartClient.send(input);
                // 检查一下用户是否准备推出了
                if (chartClient.readyToQuit(input)) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
