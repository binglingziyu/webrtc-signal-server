package com.ihubin.webrtc.signal;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocketIOLauncher {

    // 用来存已连接的客户端
    private static final Map<String, SocketIOClient> clientMap = new ConcurrentHashMap<>();

    @Resource
    private SocketIOServer socketIOServer;

    /**
     * Spring IoC 容器创建之后，在加载 SocketIOLauncher Bean 之后启动
     */
    @PostConstruct
    public void start() {
        socketIOServer.addNamespace("/");
        // 监听客户端连接
        socketIOServer.addConnectListener(client -> {
            String loginUserNum = getParamsByClient(client);
            System.out.println("New Connection!");
            if (loginUserNum != null) {
                System.out.println(loginUserNum);
                System.out.println("SessionId:  " + client.getSessionId());
                System.out.println("RemoteAddress:  " + client.getRemoteAddress());
                System.out.println("Transport:  " + client.getTransport());
                clientMap.put(loginUserNum, client);
            }
        });

        // 监听客户端断开连接
        socketIOServer.addDisconnectListener(client -> {
            String loginUserNum = getParamsByClient(client);
            System.out.println("Close Connection!");
            if (loginUserNum != null) {
                clientMap.remove(loginUserNum);
                System.out.println("断开连接： " + loginUserNum);
                System.out.println("断开连接： " + client.getSessionId());
                client.disconnect();
            }
        });

        // 处理自定义的事件，与连接监听类似
        socketIOServer.addEventListener("text", String.class, (client, data, ackSender) -> {
            // TODO do something
            client.getHandshakeData();
            System.out.println( " 客户端：************ " + data);
        });

        socketIOServer.start();
    }

    /**
     * Spring IoC 容器在销毁 SocketIOLauncher Bean 之前关闭,避免重启项目服务端口占用问题
     */
    @PreDestroy
    public void stop() {
        if (socketIOServer != null) {
            socketIOServer.stop();
            socketIOServer = null;
        }
    }

    /**
     * 此方法为获取client连接中的参数，可根据需求更改
     * @param client
     * @return
     */
    private String getParamsByClient(SocketIOClient client) {
        // 从请求的连接中拿出参数（这里的loginUserNum必须是唯一标识）
        Map<String, List<String>> params = client.getHandshakeData().getUrlParams();
        List<String> list = params.get("loginUserName");
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

}
