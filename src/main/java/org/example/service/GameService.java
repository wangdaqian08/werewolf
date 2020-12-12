package org.example.service;

import org.apache.commons.lang3.StringUtils;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.util.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.websockets.ChatController.PRIVATE_DESTINATION;

/**
 * Created by daqwang on 28/11/20.
 */
@Service
public class GameService {


    private final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final PlayerService playerService;
    private final VoteService voteService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public GameService(final PlayerService playerService, final VoteService voteService, final SimpMessagingTemplate simpMessagingTemplate) {
        this.playerService = playerService;
        this.voteService = voteService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * assign each player with role, and message them privately.
     *
     * @return list of players with roles
     */
    public List<StompPrincipal> deal() {
        final List<StompPrincipal> readyPlayers = playerService.getReadyPlayerList();
        List<StompPrincipal> stompPrincipals = assignRolesForPlayers(readyPlayers);
        if (CollectionUtils.isEmpty(stompPrincipals)) {
            return Collections.emptyList();
        } else {
            stompPrincipals.forEach(stompPrincipal -> {
                simpMessagingTemplate.convertAndSendToUser(stompPrincipal.getName(), PRIVATE_DESTINATION, stompPrincipal);
            });
            return stompPrincipals;
        }
    }

    /**
     * find player by given player id, if found, set ready = true and inGame = true, if not found, throw exception.
     *
     * @param playerName playerName
     * @return StompPrincipal the player
     */
    public StompPrincipal readyPlayer(final String playerName, final String nickname) {

        List<StompPrincipal> players = playerService.getPlayers();
        if (ListUtils.isEmpty(players) || StringUtils.isBlank(playerName)) {
            logger.error("can't find user");
            return null;
        }

        return players.stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .findFirst().map(
                        player -> {
                            player.setReady(true);
                            player.setInGame(true);
                            player.setNickName(nickname);
                            return player;
                        }
                ).orElseThrow(() -> new RuntimeException("can't find user: " + playerName));
    }

    /**
     * todo need to dynamic create roles, like more wolves and more villagers.
     *
     * @param size of total ready players
     * @return List  the size of roles equals ready players.
     */
    public List<Role> createRoles(int size) {


        List<Role> featureRoleList = new ArrayList<>();
        featureRoleList.add(Role.SEEER);
        featureRoleList.add(Role.WITCH);
        featureRoleList.add(Role.WOLF);
        featureRoleList.add(Role.WOLF);

        List<Role> roleList = new ArrayList<>(size);

        // size - 4 means, after above 4 roles, the number of remaining ready players
        for (int i = 0; i < size - featureRoleList.size(); i++) {
            roleList.add(Role.VILLAGER);
        }
        roleList.addAll(featureRoleList);
        return roleList;
    }


    /**
     * game condition check.
     *
     * @param remainPlayers player who still in game.
     * @return result of game, true:continue, false:game finish
     */
    public Map<Boolean, String> isGameFinished(List<StompPrincipal> remainPlayers) {

        List<StompPrincipal> remainWolves = remainPlayers.stream().filter(remainPlayer -> remainPlayer.getRole().equals(Role.WOLF))
                .collect(Collectors.toList());

        List<StompPrincipal> remainVillagers = remainPlayers.stream().filter((StompPrincipal remainPlayer) -> remainPlayer.getRole().equals(Role.VILLAGER))
                .collect(Collectors.toList());

        List<StompPrincipal> remainFeatures = remainPlayers.stream().filter((StompPrincipal remainPlayer) -> {
            return !remainPlayer.getRole().equals(Role.VILLAGER) && !remainPlayer.getRole().equals(Role.WOLF);
        }).collect(Collectors.toList());

        Map<Boolean, String> results = new HashMap<>();
        String message = "Game continue";
        results.put(false, message);

        //check winning condition

        if (remainWolves.size() == 0) {
            message = "Villagers Win";
            results.put(true, message);
            return results;
        }

        if (remainFeatures.size() == 0 || remainVillagers.size() == 0) {
            message = "Wolves Win";
            results.put(true, message);
            return results;
        }

        return results;
    }

    private List<StompPrincipal> assignRolesForPlayers(final List<StompPrincipal> players) {

        if (ListUtils.isEmpty(players) || !playerService.hasMinimumReadyPlayer(players)) {
            logger.error("invalid number of ready players: {},need at least 7 players.", players.size());
            return null;
        }
        List<Role> roleList = createRoles(players.size());
        Collections.shuffle(roleList);
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRole(roleList.get(i));
        }
        return players;
    }
}
