package org.example.handler;

import org.example.model.StompPrincipal;
import org.example.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;

/**
 * Created by daqwang on 24/11/20.
 */
@Component
public class PlayerHandshakeHandler extends DefaultHandshakeHandler {

    private Logger logger = LoggerFactory.getLogger(PlayerHandshakeHandler.class);
    private final PlayerService playerService;

    @Autowired
    public PlayerHandshakeHandler(PlayerService playerService){
        this.playerService = playerService;
    }



    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        StompPrincipal player = playerService.createPlayer();
        logger.info("player:{},created",player.getName());
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest
                    = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest
                    .getServletRequest().getSession();
            attributes.put("sessionId", session.getId());
        }
        return player;
    }
}
