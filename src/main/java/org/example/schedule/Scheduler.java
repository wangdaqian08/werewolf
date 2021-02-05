package org.example.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.model.GameMessage;
import org.example.model.GameResult;
import org.example.model.StompPrincipal;
import org.example.model.VoteReport;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.example.utils.EndpointConstant.BROADCAST_DESTINATION;
import static org.example.utils.EndpointConstant.BROADCAST_VOTE_RESULT_DESTINATION;

/**
 * Created by daqwang on 12/12/20.
 */
@Component
@Slf4j
public class Scheduler {

    private static final int TIME_TO_COUNTDOWN_IN_SECONDS = 1;
    private final GameService gameService;
    private final PlayerService playerService;
    private final VoteService voteService;
    private final GameStepService gameStepService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final VoiceOutputService voiceOutputService;

    @Autowired
    public Scheduler(final VoiceOutputService voiceOutputService, final GameService gameService, final PlayerService playerService, final GameStepService gameStepService, final VoteService voteService, final SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.playerService = playerService;
        this.gameStepService = gameStepService;
        this.voteService = voteService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.voiceOutputService = voiceOutputService;
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 1000)
    public void assignRoles() throws InterruptedException {

        if (!isRoleAssigned()) {
            simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage("Assign roles in " + TIME_TO_COUNTDOWN_IN_SECONDS + " seconds..."));
            Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_TO_COUNTDOWN_IN_SECONDS));
            List<StompPrincipal> readyPlayerList = playerService.getReadyPlayerList();
            if (!CollectionUtils.isEmpty(readyPlayerList) && playerService.getTotalPlayers() == readyPlayerList.size()) {
                gameService.deal();
                simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage("Roles have been assigned!"));
                gameStepService.startGame();
            }
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 60000)
    public void broadcastVoteResult() {
        VoteReport voteReport = voteService.checkVoteStatus();

        if (voteReport.getVoteCompleted()) {

            voteReport = voteService.generateVoteResultAndAnnounce();
            log.info("vote completed, VoteReport:{}", voteReport);
            simpMessagingTemplate.convertAndSend(BROADCAST_VOTE_RESULT_DESTINATION, voteReport);
            voteService.resetInGamePlayerVote();
            voteService.broadcastVoteStatus();
            VoteReport.reset();
            GameResult gameResult = gameService.isGameFinished(playerService.getInGamePlayers());
            simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage(gameResult.getMessage()));
            if (gameResult.isFinished()) {
                voiceOutputService.announceWinner(gameResult);
                log.info("######### Game Finished #########");
                return;
            } else {
                if (!gameStepService.getIsGameStarted().get() && !voteReport.getIsDraw()) {
                    startGameAgain();
                }
            }
        }
    }


    public boolean isRoleAssigned() {
        List<StompPrincipal> readyPlayerList = playerService.getReadyPlayerList();
        if (!CollectionUtils.isEmpty(readyPlayerList) && playerService.getTotalPlayers() == readyPlayerList.size()) {
            //check each ready player has roles
            Optional<StompPrincipal> anyPlayerNoRoleAssigned = readyPlayerList.stream().filter(readyPlayer -> readyPlayer.getRole() == null).findAny();
            //return false: if any player doesn't have a role
            return !anyPlayerNoRoleAssigned.isPresent();
        }
        return true;
    }


    public void startGameAgain() {
        voteService.broadcastVoteStatus();
        simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage("start next round..."));
        gameStepService.initGameActions();
        try {
            gameStepService.startGame();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
