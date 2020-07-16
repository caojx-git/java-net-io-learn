package caojx.learn.socket.aio.echo;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务器端使用回调的方式来实现异步-aio
 *
 * @author caojx created on 2020/6/27 12:09 下午
 */
public class EchoServer {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;

    /**
     * 服务器端的异步通道
     */
    AsynchronousServerSocketChannel serverChannel;


    /**
     * 关闭资源
     *
     * @param close
     */
    private void close(Closeable close) {
        if (null != close) {
            try {
                close.close();
                System.out.println("关闭" + close);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            // 绑定监听端口
            // 里面有一个AsynchronousChannelGroup，类似一个线程池，提供一些异步的通道，可以共享的一些系统资源
            serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT);

            /*
             * 等待并接收新的客户端的连接请求由于 serverChannel.accept()是异步的调用，即等不到真正的结果完成，
             * 也就是说我在调用serverChannel.accept()的时候可能完全没有客户端发送过来连接请求，
             * 即使这样，我们的调用也会立即返回，因为他是异步的调用，返回之后，我们要等到直到有客户端
             * 发来连接请求的时候，我们定义的AcceptHandler里边的回调函数才会被系统调用，
             * 即我们要保证我们服务器端的主线程还在工作，所以需要将 serverChannel.accept(null, new AcceptHandler()); 放到while循环中
             */
            while (true) {
                // attachment：附加信息，可以是任意对象类型，这里用于告诉 ClientHandler 是写操作还是读操作
                // AcceptHandler：CompletionHandler的实现，用来处理accept结束时的结果
                serverChannel.accept(null, new AcceptHandler());
                // 阻塞式调用，避免while循环过于频繁
                System.in.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    /**
     * 创建AcceptHandler，用来处理accept函数的异步调用的返回结果，即接收客户端的连接请求后，进行回调
     * <p>
     * 由AsynchronousChannelGroup中的线程回调
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        /**
         * 异步调用函数成功返回时调用
         *
         * @param result     与服务端建立连接的异步的客户端通道
         * @param attachment 额外的信息或数据
         */
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            if (serverChannel.isOpen()) {
                // 服务端接着等待下一个客户端来连接的请求
                serverChannel.accept(null, this);  // 底层限制了accept里边调用accept的层级，保证了不会出现 stackOverflow 的错误
            }

            // 处理读写操作
            AsynchronousSocketChannel clientChannel = result;
            if (null != clientChannel && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String, Object> attachmentInfo = new HashMap();
                attachmentInfo.put("type", "read");
                attachmentInfo.put("buffer", buffer);

                // 读取客户端发送的消息到buffer中，并交给ClientHandler处理把消息转发回客户端
                clientChannel.read(buffer, attachmentInfo, handler);
            }
        }

        /**
         * 异步调用失败的时候调用
         *
         * @param exc
         * @param attachment
         */
        @Override
        public void failed(Throwable exc, Object attachment) {
            // 处理一些错误的情况
        }
    }

    /***
     * 创建客户端的ClientHandler，用来处理read函数的异步调用的返回结果
     */
    private class ClientHandler implements CompletionHandler<Integer, Object> {

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            Map<String, Object> info = (Map<String, Object>) attachment;
            // 判断是读操作还是写操作
            String type = (String) info.get("type");
            // 如果是读操作，读取到数据后，将数据写回客户端
            if ("read".equals(type)) {
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                // 将 buffer 从写变为读模式
                buffer.flip();
                info.put("type", "write");
                clientChannel.write(buffer, info, this);
                buffer.clear();
            }
            // 如果之前已经把客户端发送过来的数据又重新发回给了客户端，则继续去调用read函数，继续去监听客户端发送过来的数据
            else if ("write".equals(type)) {
                // 又去读客户端发送过来的数据
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                info.put("type", "read");
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, this);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            // 处理一些错误的情况
        }
    }

    public static void main(String[] args) {
        EchoServer server = new EchoServer();
        server.start();
    }

}
