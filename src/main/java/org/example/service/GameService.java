package org.example.service;

import org.apache.commons.lang3.StringUtils;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ListUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by daqwang on 28/11/20.
 */
@Service
public class GameService {


    private Logger logger = LoggerFactory.getLogger(GameService.class);
    private final PlayerService playerService;

    @Autowired
    public GameService(final PlayerService playerService) {
        this.playerService = playerService;

    }

    public Map<String, Role> deal() {
        List<StompPrincipal> players = playerService.getPlayers();
        playerService.assignRolesForPlayers(players);
        return null;
    }

    public StompPrincipal readyPlayer(final String userId) {

        logger.info("GameService:"+playerService);
        List<StompPrincipal> players = playerService.getPlayers();
        if (ListUtils.isEmpty(players) || StringUtils.isBlank(userId)) {
            logger.error("can't find user");
            return null;
        }

        return players.stream()
                .filter(player -> player.getName().equalsIgnoreCase(userId))
                .findFirst().map(
                        player -> {
                            player.setReady(true);
                            return player;
                        }
                ).orElseThrow(() -> new RuntimeException("can't find user: " + userId));
    }

    public List<StompPrincipal> showPlayersStatus() {
        if (ListUtils.isEmpty(playerService.getPlayers())) {
            logger.warn("no players");
            return null;
        }
        return playerService
                .getPlayers()
                .stream()
                .collect(Collectors
                        .toList());
    }
}
