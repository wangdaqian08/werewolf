package org.example.action;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.service.PlayerService;
import org.example.service.VoiceOutputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.example.utils.EndpointConstant.PRIVATE_SEER_ACTION_DESTINATION;

/**
 * Created by daqwang on 20/12/20.
 */

@Component
@Slf4j
public class SeerAction extends AbstractGameAction {

    private final static String SEER_ACTION_MESSAGE = "Seer you can check the identity of a player";
    private final static String SEER_ACTION_CLOSE_EYES_MESSAGE = "Seer please close your eyes";
    private final VoiceOutputService voiceOutputService;
    private STATUS status;


    @Autowired
    public SeerAction(final PlayerService playerService, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {
        super(playerService, simpMessagingTemplate);
        this.voiceOutputService = voiceOutputService;
        this.setStatus(STATUS.READY);
    }


    @Override
    public Object call() throws Exception {
        setStatus(STATUS.IN_PROGRESS);

        sendPrivateRoleMessageToPlayer(PRIVATE_SEER_ACTION_DESTINATION, SEER_ACTION_MESSAGE, Role.SEER);
        // TODO 20/12/20
        // voiceOutputService.speak(SEER_ACTION_MESSAGE)
        log.info("SEER Action Started");

        while (!isActionCompleted(Role.SEER)) {
            // block the thread if this action haven't completed
            try {
                Thread.sleep(800L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<StompPrincipal> inGameSeer = playerService.getInGamePlayersByRole(Role.SEER);
        inGameSeer.forEach(seer -> {
            // reset wolf hasVoted field after wolf kill action
            seer.setHasVoted(false);
        });

        log.info("Seer Action Completed");
        setStatus(STATUS.FINISHED);
        resetVoteForRole(playerService, Role.SEER);

        // TODO 20/12/20
        voiceOutputService.speak(SEER_ACTION_CLOSE_EYES_MESSAGE);
        return true;
    }
}
