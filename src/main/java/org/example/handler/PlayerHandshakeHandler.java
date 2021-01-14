package org.example.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.model.StompPrincipal;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Created by daqwang on 24/11/20.
 */
@Component
@Slf4j
public class PlayerHandshakeHandler extends DefaultHandshakeHandler {

    private final PlayerService playerService;

    @Autowired
    public PlayerHandshakeHandler(PlayerService playerService) {
        this.playerService = playerService;
    }


    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest
                    = (ServletServerHttpRequest) request;
            String sessionId = servletRequest
                    .getServletRequest().getSession().getId();
            // TODO 26/12/20
            // need to remove the testValue when resolve duplicate players issue or at production
            String testValue = String.valueOf(System.currentTimeMillis());
            String sessionId2 = sessionId + testValue;
            StompPrincipal player = playerService.createPlayer(sessionId2);
            log.info("player:{},created", player.getName());
            attributes.put("sessionId", sessionId);
            return player;
        }
        return null;
    }
}
