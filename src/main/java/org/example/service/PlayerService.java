package org.example.service;

import lombok.Data;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.utils.UserIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.util.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by daqwang on 28/11/20.
 */
@Data
@Service
public class PlayerService {

    private Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private int currentPlayers;
    private List<StompPrincipal> players;
    @Value("${total.player.number}")
    private int totalPlayers;

    public PlayerService() {
        if (players == null) {
            players = new ArrayList<>();
        }
    }


    public StompPrincipal createPlayer() {
        String userId = UserIdGenerator.generateUserId();
        StompPrincipal stompPrincipal = new StompPrincipal(userId);
        while (players.contains(stompPrincipal)) {
            stompPrincipal.setName(UserIdGenerator.generateUserId());
        }
        players.add(stompPrincipal);
        return stompPrincipal;
    }


    public void cleanAllUsers() {
        if (!ListUtils.isEmpty(players)) {
            players.clear();
        }
    }

    public List<StompPrincipal> getReadyPlayerList() {
        return players.stream().filter(StompPrincipal::isReady).collect(Collectors.toList());
    }

    public List<StompPrincipal> getInGamePlayersByRole(final Role role) {
        return this.getReadyPlayerList().stream()
                .filter(player -> player.getRole().equals(role) && player.isInGame())
                .collect(Collectors.toList());
    }

    boolean hasMinimumReadyPlayer(List<StompPrincipal> players) {
        List<StompPrincipal> readyList = players.stream().filter(StompPrincipal::isReady).collect(Collectors.toList());
        return readyList.size() >= 7;
    }


    public List<StompPrincipal> showPlayersStatus() {
        if (ListUtils.isEmpty(getPlayers())) {
            logger.warn("no players");
            return null;
        }
        return getPlayers();
    }


    public StompPrincipal getPlayerByName(final String name) {
        if (CollectionUtils.isEmpty(players)) {
            throw new RuntimeException("player list not ready,can't find name: " + name);
        }
        Optional<StompPrincipal> optionalStompPrincipal = players.stream().filter(player -> player.getName().equalsIgnoreCase(name)).findFirst();
        return optionalStompPrincipal.orElse(null);
    }


    /**
     * reset all ready players' vote count to 0
     *
     * @return ready players
     */
    public List<StompPrincipal> resetVoteCount() {
        List<StompPrincipal> readyPlayerList = this.getReadyPlayerList();
        if (CollectionUtils.isEmpty(readyPlayerList)) {
            throw new RuntimeException("no ready players yet");
        }
        readyPlayerList.forEach(readyPlayer -> readyPlayer.setVoteCount(0));
        return readyPlayerList;
    }
}
