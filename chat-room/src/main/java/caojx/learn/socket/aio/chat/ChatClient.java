package caojx.learn.socket.aio.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

/**
 * 使用AIO编程模型实现多人聊天室-客户端
 */
public class ChatClient {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    /**
     * 异步通道
     */
    private AsynchronousSocketChannel clientChannel;

    /**
     * 编码方式
     */
    private Charset charset = Charset.forName("UTF-8");


    private void start() {

        try {
            // 创建异步通道channel，并发起连接请求
            clientChannel = AsynchronousSocketChannel.open();
            Future<Void> future = clientChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));

            // 阻塞式调用，等待客户端连接成功
            future.get();
            System.out.println("已连接到服务器");

            // 处理用户输入事件
            new Thread(new UserInputHandler(this)).start();

            // 主线程中循环中读取服务器转发过来的其他客户端消息
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
            while (true) {
                Future<Integer> readResult = clientChannel.read(buffer);
                // 阻塞式读取数据
                int result = readResult.get();
                if (result <= 0) {
                    // 发生异常，没有读取到数据
                    close(clientChannel);
                    System.out.println("与服务器连接异常");
                    System.exit(1);
                } else {
                    // 正常打印消息
                    buffer.flip();
                    String message = String.valueOf(charset.decode(buffer));
                    buffer.clear();
                    System.out.println(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(clientChannel);
        }
    }

    /**
     * 向服务器发送消息
     *
     * @param message
     * @throws Exception
     */
    public void send(String message) throws Exception {
        if (message.isEmpty()) {
            return;
        }
        ByteBuffer byteBuffer = charset.encode(message);
        Future<Integer> writeResult = clientChannel.write(byteBuffer);
        writeResult.get();
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }


    /**
     * 回收资源
     *
     * @param closeable
     */
    public void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
}
