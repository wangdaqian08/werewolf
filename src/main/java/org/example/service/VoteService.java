package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.action.WitchAction;
import org.example.model.ActionResult;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.model.VoteReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.action.WitchAction.ANTIDOTE;
import static org.example.action.WitchAction.POISON;
import static org.example.service.GameService.RoleAction.*;

/**
 * Created by daqwang on 12/12/20.
 */
@Service
@Slf4j
public class VoteService {


    private final PlayerService playerService;
    private final WitchAction witchAction;

    @Autowired
    public VoteService(final PlayerService playerService, final WitchAction witchAction) {
        this.playerService = playerService;
        this.witchAction = witchAction;
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
            player.voteBy(voter);
        } else {
            throw new RuntimeException("Player: " + voter.getName() + " no longer in game,can't vote");
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

    public VoteReport generateVoteResult() {

        //players who still in game
        List<StompPrincipal> remainPlayers = playerService.getReadyPlayerList().stream().filter(StompPrincipal::isInGame).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(remainPlayers)) {
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
            List<StompPrincipal> drawList = remainPlayers.stream().filter(remainPlayer -> remainPlayer.getVoteCount().intValue() == max.intValue())
                    .collect(Collectors.toList());
            voteReport.setDrawList(drawList);
            voteReport.setIsDraw(true);
            String nameOfList = drawList.stream().map(StompPrincipal::getNickName).collect(Collectors.joining(", "));
            voteReport.setMessage(String.format("Players: %s, have some amount of votes: %d", nameOfList, max));
        } else {
            voteReport.setIsDraw(false);
            voteReport.setDrawList(Collections.emptyList());
            StompPrincipal stompPrincipal = remainPlayers.stream().filter(remainPlayer -> remainPlayer.getVoteCount().intValue() == max)
                    .findFirst().orElseThrow(() -> new RuntimeException("can't find the highest vote"));
            stompPrincipal.setInGame(false);
            voteReport.setMessage("Player: " + stompPrincipal.getNickName() + " out! ");
            // TODO 12/12/20 broadcast user status, private user status

        }
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

    public ActionResult handleRoleAction(final StompPrincipal voter, final String nickName, final GameService.RoleAction roleAction) {

        switch (roleAction) {
            case CHECK:
                return handleSeerCheckAction(voter, nickName);
            case KILL:
                return handleWolfKillAction(voter, nickName);
            case SAVE:
                return handleWitchHelpAction(voter, nickName);
            case POISONING:
                return handleWitchPoisonAction(voter, nickName);
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
     * @param nickName player nick name need to be saved
     * @return ActionResult
     */
    private ActionResult handleWitchHelpAction(final StompPrincipal voter, final String nickName) {

        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        ActionResult witchHelpActionResult = ActionResult.getInstance("handle witch help action");
        inGamePlayers.stream()
                .filter(player -> player.getNickName().equalsIgnoreCase(nickName))
                .findFirst()
                .ifPresentOrElse((player) -> {

                    if (witchAction.consumeAvailableWitchItem(ANTIDOTE)) {
                        witchHelpActionResult.getResultPlayer().put(player, SAVE);
                        witchHelpActionResult.setResult("witch finish the help action");
                    } else {
                        witchHelpActionResult.setResult("witch can't finish the help action, no available " + ANTIDOTE);
                    }
                    voter.setHasVoted(true);
                }, () -> {
                    throw new RuntimeException("can't find player " + nickName + "to check");
                });
        return witchHelpActionResult;
    }


    /**
     * handle witch poison action for player, by the play nickname.<br/>
     * set player inGame is false<br/>
     * set voter is voted to true<br/>
     *
     * @param voter    the witch player
     * @param nickName the player's nickname to be poisoned
     * @return ActionResult
     */
    private ActionResult handleWitchPoisonAction(StompPrincipal voter, String nickName) {
        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        ActionResult witchPoisonActionResult = ActionResult.getInstance("handle witch poison action");
        inGamePlayers.stream()
                .filter(player -> player.getNickName().equalsIgnoreCase(nickName))
                .findFirst()
                .ifPresentOrElse((player) -> {
                    if (witchAction.consumeAvailableWitchItem(POISON)) {
                        witchPoisonActionResult.getResultPlayer().put(player, POISONING);
                        witchPoisonActionResult.setResult("witch finish the poison action");
                    } else {
                        witchPoisonActionResult.setResult("no available " + POISON + " available");
                    }
                    voter.setHasVoted(true);
                }, () -> {
                    throw new RuntimeException("can't find player " + nickName + "to check");
                });
        return witchPoisonActionResult;
    }

    /**
     * handle seer check action by player nickname.
     * set seer isVoted=true, return the checked player object
     *
     * @param voter    seer
     * @param nickName the player's nickname need to be checked
     * @return ActionResult contains the checked player
     */
    private ActionResult handleSeerCheckAction(StompPrincipal voter, String nickName) {
        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        ActionResult seerActionResult = ActionResult.getInstance("handle seer check action");
        inGamePlayers.stream()
                .filter(player -> player.getNickName().equalsIgnoreCase(nickName))
                .findFirst()
                .ifPresentOrElse((player) -> {
                    seerActionResult.getResultPlayer().put(player, CHECK);
                    seerActionResult.setResult("seer finish the check action");
                    voter.setHasVoted(true);
                }, () -> {
                    throw new RuntimeException("can't find player " + nickName + "to check");
                });
        return seerActionResult;
    }

    /**
     * handle wolf kill action by player nick name.
     *
     * @param voter    the wolf player
     * @param nickName the player will be killed
     * @return ActionResult
     */
    private ActionResult handleWolfKillAction(final StompPrincipal voter, final String nickName) {

        List<StompPrincipal> inGamePlayers = playerService.getInGamePlayers();
        final ActionResult wolfActionResult = ActionResult.getInstance("handle wolf kill action");
        inGamePlayers.stream()
                .filter(player -> player.getNickName().equalsIgnoreCase(nickName))
                .findFirst()
                .ifPresentOrElse((player) -> {
                    wolfActionResult.getResultPlayer().put(player, KILL);
                    wolfActionResult.setResult("wolf finish the kill action");
                    voter.setHasVoted(true);
                }, () -> {
                    //todo need to refactor this code: second wolf vote for the same player can't be found, need to update the second wolf isVote to true
                    if (playerService.getInGamePlayersByRole(Role.WOLF).stream().filter(StompPrincipal::isHasVoted).collect(Collectors.toList()).size() == 1) {
                        voter.setHasVoted(true);
                    }
                    log.info("can't find player {} to kill", nickName);
                });
        return wolfActionResult;
    }
}
