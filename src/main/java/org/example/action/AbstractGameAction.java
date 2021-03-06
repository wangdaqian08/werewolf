package org.example.action;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.model.GameMessage;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by daqwang on 20/12/20.
 */
@Data
@Component
@Slf4j
public abstract class AbstractGameAction implements Callable<Object> {


    protected final PlayerService playerService;
    protected final SimpMessagingTemplate simpMessagingTemplate;
    protected STATUS status;
    private LinkedList<AbstractGameAction> gameActionList = new LinkedList<>();
    private Map<GameService.RoleAction, StompPrincipal> witchActionPlayer = new HashMap<>();

    @Autowired
    public AbstractGameAction(final PlayerService playerService, final SimpMessagingTemplate simpMessagingTemplate) {
        this.playerService = playerService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * each game action implements by itself, as different action has different criteria of finish.
     *
     * @return true: the game action(game step) is completed(all available voters have voted)/no player with the given role <br/>
     * false: the game action(game step) is not yet completed.<br/>
     * the isHasVoted attribute is set by
     */
    protected boolean isActionCompleted(final Role role) {
        List<StompPrincipal> inGameRolePlayers = playerService.getInGamePlayersByRole(role);

        if (CollectionUtils.isEmpty(inGameRolePlayers)) {
            //no wolves left in Game, action is completed
            return true;
        } else {
            int numberOfVoted = (int) inGameRolePlayers.stream().filter(StompPrincipal::isHasVoted).count();
            // check if all in game players with roles have voted
            boolean isFinished = inGameRolePlayers.size() == numberOfVoted;
            if (isFinished) {
                // TODO 27/12/20
                // add
                //if all players with the given are voted, need to reset the vote flag the for next round.
                this.resetVoteForRole(playerService, role);
            }
            return isFinished;
        }
    }

    /**
     * reset the players vote flag to <b>false</b>, as the players already voted, this step is prepare for next round of vote.
     *
     * @param playerService to get players who are in game with desired role
     * @param role          specific the type of player to reset the vote
     */
    protected void resetVoteForRole(final PlayerService playerService, final Role role) {
        playerService.getReadyPlayerList().stream()
                .filter(player -> player.isInGame() && player.getRole().equals(role))
                .forEach(player -> player.setHasVoted(false));
    }

    /**
     * each feature player may need to send private message to the player, asking for action(Poison Player, Check Player Identity, Kill Player).
     *
     * @param actionDestination private channel for client role user to receive message
     * @param actionMessage     role specific message will be sent
     * @param role              which type of role should be filtered to receive message
     */
    protected void sendPrivateRoleMessageToPlayer(final String actionDestination, final String actionMessage, final Role role) {


        final GameMessage gameMessage = new GameMessage(actionMessage);
        //could be list of players to be killed, or saved, poisoned, and checked.
        List<StompPrincipal> candidatePlayers = playerService.getReadyPlayerList().stream()
                .filter(player -> player.isInGame() && !player.getRole().equals(role))
                .collect(Collectors.toList());
        gameMessage.setCandidatePlayers(candidatePlayers);

        List<StompPrincipal> inGamePlayersByRole = playerService.getInGamePlayersByRole(role);
        if (!CollectionUtils.isEmpty(inGamePlayersByRole)) {
            inGamePlayersByRole.forEach(player -> {
                        simpMessagingTemplate.convertAndSendToUser(player.getName(), actionDestination, gameMessage);
                        log.info("send message to:" + role + "," + player.getName());
                    }
            );

        }
    }

    public enum STATUS {
        /**
         * ready action status, this step haven't started yet,
         */
        READY,
        /**
         * this step is started and waiting client side response.
         */
        IN_PROGRESS,
        /**
         * this step is executed and finished.
         */
        FINISHED
    }
}
