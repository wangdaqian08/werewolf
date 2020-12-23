package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.StompPrincipal;
import org.example.model.VoteReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by daqwang on 12/12/20.
 */
@Service
@Slf4j
public class VoteService {


    private final PlayerService playerService;

    @Autowired
    public VoteService(final PlayerService playerService) {
        this.playerService = playerService;
    }

    /**
     * player vote the others players based on speech.
     *
     * @param voter      the player who make the vote
     * @param playerName the player who will be voted
     */
    public void vote(final StompPrincipal voter, final String playerName) {

        //only inGame user can vote and vote only inGame players
        StompPrincipal player = playerService.getPlayerByName(playerName);
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
}
