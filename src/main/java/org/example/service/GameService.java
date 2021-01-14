package org.example.service;

import org.apache.commons.lang3.StringUtils;
import org.example.model.GameResult;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.utils.EndpointConstant.PRIVATE_ROLE_DESTINATION;

/**
 * Created by daqwang on 28/11/20.
 */
@Service
public class GameService {


    private final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final PlayerService playerService;
    private final VoteService voteService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * find player by given player id, if found, set ready = true and inGame = true, if not found, throw exception.
     * check total ready players, once all players are ready, start game, (efficient than scheduled job for )
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

        StompPrincipal stompPrincipal = players.stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .findFirst().map(
                        player -> {
                            player.setReady(true);
                            player.setInGame(true);
                            player.setNickName(nickname);
                            return player;
                        }
                ).orElseThrow(() -> new RuntimeException("can't find user: " + playerName));

        if (playerService.getReadyPlayerList().size() == playerService.getTotalPlayers()) {
            // TODO 18/12/20
            // assignRoles()
        }
        return stompPrincipal;
    }

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
                simpMessagingTemplate.convertAndSendToUser(stompPrincipal.getName(), PRIVATE_ROLE_DESTINATION, stompPrincipal);
            });
            return stompPrincipals;
        }
    }

    /**
     * find the role for the given player nickName who is ready and inGame.
     *
     * @param nickName of the player to be checked
     * @return Role of the player
     */
    public Role checkPlayerRole(final String nickName) {
        StompPrincipal stompPrincipal = playerService.getReadyPlayerList().stream().filter(readyPlayer -> readyPlayer.isInGame()).filter(readyPlayer -> readyPlayer.getNickName().equalsIgnoreCase(nickName)).findFirst().orElse(null);
        if (stompPrincipal != null) {
            return stompPrincipal.getRole();
        }
        return null;
    }

    /**
     * todo need to dynamic create roles, like more wolves and more villagers.
     *
     * @param size of total ready players
     * @return List  the size of roles equals ready players.
     */
    public List<Role> createRoles(int size) {


        List<Role> featureRoleList = new ArrayList<>();
        featureRoleList.add(Role.SEER);
        featureRoleList.add(Role.WITCH);
        featureRoleList.add(Role.WOLF);
        featureRoleList.add(Role.WOLF);

        List<Role> roleList = new ArrayList<>(size);

        // (size - 4) means, after above 4 roles, the number of remaining ready players
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
    public GameResult isGameFinished(List<StompPrincipal> remainPlayers) {

        List<StompPrincipal> remainWolves = remainPlayers.stream().filter(remainPlayer -> remainPlayer.getRole().equals(Role.WOLF))
                .collect(Collectors.toList());

        List<StompPrincipal> remainVillagers = remainPlayers.stream().filter((StompPrincipal remainPlayer) -> remainPlayer.getRole().equals(Role.VILLAGER))
                .collect(Collectors.toList());

        List<StompPrincipal> remainFeatures = remainPlayers.stream().filter((StompPrincipal remainPlayer) -> !remainPlayer.getRole().equals(Role.VILLAGER) && !remainPlayer.getRole().equals(Role.WOLF)).collect(Collectors.toList());

        GameResult results = new GameResult(false, "Game continue");

        //check winning condition

        if (remainWolves.size() == 0) {
            results.setMessage("Villagers Win");
            results.setFinished(true);
            return results;
        }

        if (remainFeatures.size() == 0 || remainVillagers.size() == 0) {
            results.setMessage("Wolves Win");
            results.setFinished(true);
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

    public enum RoleAction {
        /**
         * witch action: kill a player using poison
         */
        POISONING,

        /**
         * witch action to save the player using antidote
         */
        SAVE,

        /**
         * wolf action to kill the player
         */
        KILL,

        /**
         * seer action to check the player's identity
         */
        CHECK,

        /**
         * every player vote for the player your suspect
         */
        VOTE,

        /**
         * witch choose do nothing
         */
        NOTHING
    }
}
