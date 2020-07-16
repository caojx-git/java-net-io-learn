package processor;

import connector.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 处理servlet动态资源请求
 */
public class ServletProcessor {

    /**
     * 加载webroot下的servlet
     *
     * @return
     * @throws MalformedURLException
     */
    URLClassLoader getServletLoader() throws MalformedURLException {
        File webRoot = new File(ConnectorUtils.WEB_ROOT);
        URL webRootUrl = webRoot.toURI().toURL();
        return new URLClassLoader(new URL[]{webRootUrl});
    }

    /**
     * 获取servlet实例
     *
     * @param loader
     * @param request
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    Servlet getServlet(URLClassLoader loader, Request request) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        /*
         /servlet/TimeServlet--> TimeServlet
        */
        String uri = request.getRequestURI();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);

        // 返回对应的实例
        Class servletClass = loader.loadClass(servletName);
        Servlet servlet = (Servlet) servletClass.newInstance();
        return servlet;
    }

    /**
     * 加载Servlet类，并通过反射获取实例，之后调用service方法
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void process(Request request, Response response) throws IOException {
        URLClassLoader loader = getServletLoader();
        try {
            // 获取servlet实例
            Servlet servlet = getServlet(loader, request);

//            servlet.service(request, response);

            // 使用外观模式包裹request和response，避免用户可以直接访问request或response中非Servlet的方法
            RequestFacade requestFacade = new RequestFacade(request);
            ResponseFacade responseFacade = new ResponseFacade(response);

            // 传入外观模式包装的对象，避免后续用户可以直接访问Request对象中的方法
            servlet.service(requestFacade, responseFacade);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}