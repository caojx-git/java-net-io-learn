package processor;



import connector.Request;
import connector.Response;

import java.io.IOException;

/**
 * 处理静态的资源请求
 */
public class StaticProcessor {

    public void process(Request request, Response response) {
        try {
            response.sendStaticResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}