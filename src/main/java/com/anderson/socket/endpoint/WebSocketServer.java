package com.anderson.socket.endpoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fengky
 */
@ServerEndpoint(value = "/webSocketServer")
@Component
@Slf4j
public class WebSocketServer {

    private Session session;
    private static List<WebSocketServer> clientList = new ArrayList<>();

    public WebSocketServer(){
        System.out.println("我new出来啦 WebSocketServer");
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("onOpen");
        clientList.add(this);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("onMessage: {}", message);
    }

    @OnError
    public void onError(Throwable error, Session session) {
        log.info("onError {}", error);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("onClose");

    }

    public static void sendMessageToAll(String message) {
        for (WebSocketServer webSocketServer : clientList) {
            webSocketServer.session.getAsyncRemote().sendText(message);
        }
    }
}
