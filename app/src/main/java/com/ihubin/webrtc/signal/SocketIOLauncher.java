package com.ihubin.webrtc.signal;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.ihubin.webrtc.signal.model.CommandMessage;
import com.ihubin.webrtc.signal.model.SignalMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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
        // SocketIONamespace namespace = socketIOServer.addNamespace("/");
        // 监听客户端连接
        socketIOServer.addConnectListener(client -> {
            String loginUserName = getParamsByClient(client);
            log.debug("新的连接：" + loginUserName);
            if (StringUtils.isNotBlank(loginUserName)) {
                if(clientMap.containsKey(loginUserName)) {
                    log.debug("新的连接关闭了：" + loginUserName + "，用户名重复");
                    // 重名的连接直接关闭（或者可以返回重名提示）
                    client.disconnect();
                    return;
                }
                clientMap.put(loginUserName, client);
                // 连接成功，发送在线用户列表
                socketIOServer.getBroadcastOperations().sendEvent("userList", JSON.toJSON(clientMap.keySet()));
                //client.sendEvent("userList", JSON.toJSON(clientMap.keySet()));
            } else {
                log.debug("新的连接关闭了：" + loginUserName + "，没有用户名");
                // 没有用户名的连接直接关闭
                client.disconnect();
            }
        });

        // 监听客户端断开连接
        socketIOServer.addDisconnectListener(client -> {
            String loginUserName = getParamsByClient(client);
            log.debug("连接断开了：" + loginUserName);
            if (StringUtils.isNotBlank(loginUserName)) {
                clientMap.remove(loginUserName);
            }
            client.disconnect();
            socketIOServer.getBroadcastOperations().sendEvent("userList", JSON.toJSON(clientMap.keySet()));
        });

        // 处理命令的事件
        socketIOServer.addEventListener("command", CommandMessage.class, (client, data, ackSender) -> {
            HandshakeData handshakeData = client.getHandshakeData();
            log.debug( "客户端命令：" + data);
            switch (data.getType()) {
                case "getUserList":
                    client.sendEvent("userList", JSON.toJSON(clientMap.keySet()));
                    break;
                case "userContact":
                    String toUserName = data.getPayload();
                    SocketIOClient toClient = clientMap.get(toUserName);
                    toClient.sendEvent("userContact", getParamsByClient(client));
                    break;
                case "call":
                    String callUserName = data.getPayload();
                    SocketIOClient callClient = clientMap.get(callUserName);
                    callClient.sendEvent("call", getParamsByClient(client));
                    break;
                case "broadcast":
                    socketIOServer.getBroadcastOperations().sendEvent("broadcast", JSON.toJSON(clientMap.keySet()));
                    break;
                default:
                    break;
            }
        });

        // 处理消息的事件
        socketIOServer.addEventListener("message", SignalMessage.class, (client, data, ackSender) -> {
            HandshakeData handshakeData = client.getHandshakeData();
            log.debug( "客户端消息：" + data);
            String contactTo = data.getContactTo();
            SocketIOClient callClient = clientMap.get(contactTo);
            callClient.sendEvent("message", JSON.toJSON(data));
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
     * @param client SocketIOClient
     * @return String
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
