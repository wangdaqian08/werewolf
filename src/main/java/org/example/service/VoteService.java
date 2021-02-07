package org.example.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.example.action.WitchAction;
import org.example.config.VoiceProperties;
import org.example.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.action.WitchAction.ANTIDOTE;
import static org.example.action.WitchAction.POISON;
import static org.example.service.GameService.RoleAction.*;
import static org.example.utils.EndpointConstant.BROADCAST_PLAYER_STATUS_DESTINATION;

/**
 * Created by daqwang on 12/12/20.
 */
@Service
@Slf4j
public class VoteService {


    private final PlayerService playerService;
    private final WitchAction witchAction;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final VoiceOutputService voiceOutputService;

    @Autowired
    public VoteService(final VoiceOutputService voiceOutputService, final PlayerService playerService, final WitchAction witchAction, final SimpMessagingTemplate simpMessagingTemplate) {
        this.playerService = playerService;
        this.witchAction = witchAction;
        this.voiceOutputService = voiceOutputService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public List<String> getAvailableWitchItems(){
        return witchAction.generateAvailableWitchItems();
    }

    /**
     * player vote the others players based on speech.
     *
     * @param voter          the player who make the vote
     * @param playerNickName the player who will be voted
     */
    public void vote(final StompPrincipal voter, final String playerNickName) {

        //only inGame user can vote and vote only inGame players
        StompPrincipal player = playerService.getPlayerByNickName(playerNickName);
        if (voter.isInGame() && validatePlayer(player)) {
            voter.setHasVoted(true);
            player.voteBy(voter);
        } else {
            throw new RuntimeException("Player: " + player.getName() + "or Voter: " + voter.getName() + " no longer in game,can't vote");
        }
    }

    /**
     * validate the voted player is ready, inGame and the player exists.
     *
     * @param player player to be validated
     * @return true - validate player;false - invalidate user
     */
    private boolean validatePlayer(StompPrincipal player) {

        return player != null && player.isReady() && player.isInGame();
    }

    /**
     * create vote result, announce vote finished message.
     *
     * @return VoteReport
     */
    public VoteReport generateVoteResultAndAnnounce() {

        //players who still in game
        List<StompPrincipal> remainPlayers = playerService.getReadyPlayerList().stream().filter(StompPrincipal::isInGame).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(remainPlayers)) {
            throw new RuntimeException("no available players");
        }

        final VoteReport voteReport = checkVoteStatus();
        if (!voteReport.getVoteCompleted()) {
            log.warn("vote not finished yet");
            return voteReport;
        }
        // calculate vote,create vote details
        remainPlayers.forEach(remainPlayer -> voteReport.makeVoteDetails(remainPlayer, remainPlayer.getVotedBySet()));

        // check draw condition
        calculateVoteCondition(voteReport, remainPlayers);
        voiceOutputService.speak(VoiceProperties.VOTE_FINISHED_FILE_NAME);
        return voteReport;
    }

    /**
     * check draw condition. if not draw, set the highest vote player inGame status to false.
     *
     * @param voteReport    result will be set in this object
     * @param remainPlayers player list to be checked
     */
    private void calculateVoteCondition(final VoteReport voteReport, final List<StompPrincipal> remainPlayers) {

        List<Integer> voteList = remainPlayers.stream().map(StompPrincipal::getVoteCount).collect(Collectors.toList());
        Integer max = Collections.max(voteList);
        if (Collections.frequency(voteList, max) > 1) {
            //draw condition
            List<StompPrincipal> drawList = remainPlayers.stream()
                    .filter(remainPlayer -> remainPlayer.getVoteCount().intValue() == max.intValue())
                    .collect(Collectors.toList());
            voteReport.setDrawList(drawList);
            voteReport.setIsDraw(true);
            String nameOfList = drawList.stream().map(StompPrincipal::getNickName).collect(Collectors.joining(", "));
            voteReport.setMessage(String.format("Players: %s, have same amount of votes: %d", nameOfList, max));
        } else {
            voteReport.setIsDraw(false);
            voteReport.setDrawList(new ArrayList<>());
            StompPrincipal stompPrincipal = remainPlayers.stream().filter(remainPlayer -> remainPlayer.getVoteCount().intValue() == max)
                    .findFirst().orElseThrow(() -> new RuntimeException("can't find the highest vote"));
            stompPrincipal.setInGame(false);
            voteReport.setMessage("Player: " + stompPrincipal.getNickName() + " out! ");
        }
        simpMessagingTemplate.convertAndSend(BROADCAST_PLAYER_STATUS_DESTINATION, new Gson().toJson(playerService.getReadyPlayerList()));
    }

    /**
     * check remaining players has finished the vote.
     *
     * @return VoteReport
     */
    public VoteReport checkVoteStatus() {
        List<StompPrincipal> remainPlayers = playerService.getReadyPlayerList().stream().filter(StompPrincipal::isInGame).collect(Collectors.toList());
        VoteReport voteReport = VoteReport.getInstance();
        int totalVoteCount = remainPlayers.stream().filter(remainPlayer -> remainPlayer.getVoteCount() != null)
                .mapToInt(StompPrincipal::getVoteCount).sum();
        if (totalVoteCount == remainPlayers.size() && totalVoteCount != 0) {
            //vote finished
            voteReport.setVoteCompleted(true);
            voteReport.setMessage("vote completed");
            return voteReport;
        } else {
            //vote not finished
            voteReport.setVoteCompleted(false);
            voteReport.setMessage("vote not completed, total vote should be: " + remainPlayers.size() + " current vote:" + totalVoteCount);

            if (remainPlayers.size() < totalVoteCount) {
                log.error("Vote number is not match, total vote is: {}, current vote:{}", remainPlayers.size(), totalVoteCount);
                throw new RuntimeException("vote number not match, total vote is more than remaining players");
            }
        }

        return voteReport;
    }

    public ActionResult handleRoleAction(final StompPrincipal voter, final String name, final GameService.RoleAction roleAction) {

        switch (roleAction) {
            case CHECK:
                return handleSeerCheckAction(voter, name);
            case KILL:
                return handleWolfKillAction(voter, name);
            case SAVE:
                return handleWitchHelpAction(voter, name);
            case POISONING:
                return handleWitchPoisonAction(voter, name);
            case NOTHING:
                return handleWitchDoNothingAction(voter, null);
            default:
                throw new RuntimeException("can't find match action:" + roleAction.name());
        }
    }

    private ActionResult handleWitchDoNothingAction(StompPrincipal voter, Object o) {

        voter.setHasVoted(true);
        return ActionResult.getInstance("witch did nothing");
    }

    /**
     * witch save player by player nick name.
     *
     * @param name player nick name need to be saved
     * @return ActionResult
     */
    private ActionResult handleWitchHelpAction(final StompPrincipal voter, final String name) {

        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        ActionResult witchHelpActionResult = ActionResult.getInstance("handle witch help action");
        inGamePlayers.stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresentOrElse((player) -> {

                    if (witchAction.consumeAvailableWitchItem(ANTIDOTE)) {
                        witchHelpActionResult.getActionedPlayerList().add(new ExecuteAction(SAVE, player));
                        witchHelpActionResult.setResultMessage("witch finish the help action");
                    } else {
                        witchHelpActionResult.setResultMessage("witch can't finish the help action, no available " + ANTIDOTE);
                    }
                    voter.setHasVoted(true);
                }, () -> {
                    throw new RuntimeException("can't find player " + name + "to check");
                });
        return witchHelpActionResult;
    }


    /**
     * handle witch poison action for player, by the play nickname.<br/>
     * set player inGame is false<br/>
     * set voter is voted to true<br/>
     *
     * @param voter the witch player
     * @param name  the player's nickname to be poisoned
     * @return ActionResult
     */
    private ActionResult handleWitchPoisonAction(StompPrincipal voter, String name) {
        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        ActionResult witchPoisonActionResult = ActionResult.getInstance("handle witch poison action");
        inGamePlayers.stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresentOrElse((player) -> {
                    if (witchAction.consumeAvailableWitchItem(POISON)) {
                        witchPoisonActionResult.getActionedPlayerList().add(new ExecuteAction(POISONING, player));
                        witchPoisonActionResult.setResultMessage("witch finish the poison action");
                    } else {
                        witchPoisonActionResult.setResultMessage("no available " + POISON + " available");
                    }
                    voter.setHasVoted(true);
                }, () -> {
                    throw new RuntimeException("can't find player " + name + "to check");
                });
        return witchPoisonActionResult;
    }

    /**
     * handle seer check action by player nickname.
     * set seer isVoted=true, return the checked player object
     *
     * @param voter seer
     * @param name  the player's nickname need to be checked
     * @return ActionResult contains the checked player
     */
    private ActionResult handleSeerCheckAction(StompPrincipal voter, String name) {
        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        ActionResult seerActionResult = ActionResult.getInstance("handle seer check action");
        inGamePlayers.stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresentOrElse((player) -> {
                    seerActionResult.getActionedPlayerList().add(new ExecuteAction(CHECK, player));
                    seerActionResult.setResultMessage("seer finish the check action");
                    voter.setHasVoted(true);
                }, () -> {
                    throw new RuntimeException("can't find player " + name + "to check");
                });
        return seerActionResult;
    }

    /**
     * handle wolf kill action by player nick name.
     *
     * @param voter the wolf player
     * @param name  the player will be killed
     * @return ActionResult
     */
    private ActionResult handleWolfKillAction(final StompPrincipal voter, final String name) {

        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        final ActionResult wolfActionResult = ActionResult.getInstance("handle wolf kill action");
        inGamePlayers.stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresentOrElse((player) -> {
                    wolfActionResult.getActionedPlayerList().add(new ExecuteAction(KILL, player));
                    wolfActionResult.setResultMessage("wolf finish the kill action");
                    voter.setHasVoted(true);
                }, () -> {
                    //todo need to refactor this code: second wolf vote for the same player can't be found, need to update the second wolf isVote to true
                    if (playerService.getInGamePlayersByRole(Role.WOLF).stream().filter(StompPrincipal::isHasVoted).count() == 1) {
                        voter.setHasVoted(true);
                    }
                    log.info("can't find player {} to kill", name);
                });
        return wolfActionResult;
    }

    public void resetInGamePlayerVote() {
        List<StompPrincipal> inGamePlayers = playerService.getReadyPlayerList();
        inGamePlayers.forEach(player -> {
            player.setHasVoted(false);
            player.setVoteCount(0);
            player.getVotedBySet().clear();
        });

    }

    public void broadcastVoteStatus() {
        simpMessagingTemplate.convertAndSend(BROADCAST_PLAYER_STATUS_DESTINATION, new Gson().toJson(playerService.getReadyPlayerList()));
    }
}
