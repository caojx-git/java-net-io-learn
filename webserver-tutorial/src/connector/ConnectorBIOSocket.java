package connector;

import processor.ServletProcessor;
import processor.StaticProcessor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 使用BIO模型，仅用于处理连接
 */
public class ConnectorBIOSocket implements Runnable {

    private static final int DEFAULT_PORT = 8888;
    private ServerSocket server;
    private int port;

    public ConnectorBIOSocket() {
        this(DEFAULT_PORT);
    }

    public ConnectorBIOSocket(int port) {
        this.port = port;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            // 创建ServerSocket开始监听对应的服务端口
            server = new ServerSocket(port);
            System.out.println("启动服务器，监听端口：" + port);

            while (true) {
                // 创建socket
                Socket socket = server.accept();
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();

                // 创建request与response
                Request request = new Request(input);
                request.parse();

                Response response = new Response(output);
                response.setRequest(request);

                if (request.getRequestURI().startsWith("/servlet/")) {
                    // 处理动态资源请求
                    ServletProcessor processor = new ServletProcessor();
                    processor.process(request, response);
                } else {
                    // 处理静态资源请求
                    StaticProcessor processor = new StaticProcessor();
                    processor.process(request, response);
                }

                // 关闭连接
                close(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(server);
        }
    }

    private void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
