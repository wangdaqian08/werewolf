package org.example.action;

import lombok.extern.slf4j.Slf4j;
import org.example.config.VoiceProperties;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.service.PlayerService;
import org.example.service.VoiceOutputService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.example.utils.EndpointConstant.PRIVATE_WEREWOLF_ACTION_DESTINATION;

/**
 * Created by daqwang on 20/12/20.
 */
@Component
@Slf4j
public class WerewolfAction extends AbstractGameAction {


    private final static String WOLVES_ACTION_KILL_MESSAGE = "Werewolves please kill villagers";
    private final static String WOLVES_ACTION_KILL_QUESTION_MESSAGE = "Which person do you want to kill?";
    private final static String WOLVES_ACTION_CLOSE_EYES_MESSAGE = "Werewolves please close your eyes";

    private final VoiceOutputService voiceOutputService;


    public WerewolfAction(final PlayerService playerService, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {
        super(playerService, simpMessagingTemplate);
        this.voiceOutputService = voiceOutputService;
        this.setStatus(STATUS.READY);
    }

    @Override
    public Object call() {
        voiceOutputService.speak(VoiceProperties.WOLF_ACTION_FILE_NAME);
        setStatus(STATUS.IN_PROGRESS);
        sendPrivateRoleMessageToPlayer(PRIVATE_WEREWOLF_ACTION_DESTINATION, WOLVES_ACTION_KILL_QUESTION_MESSAGE, Role.WOLF);
        // TODO 20/12/20
        // voiceOutputService.speak(WOLVES_ACTION_KILL_MESSAGE)
        log.info("Werewolf Action Started");

        while (!isActionCompleted(Role.WOLF)) {

            // block the thread if this action haven't completed

            try {
                Thread.sleep(800L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<StompPrincipal> inGameWolves = playerService.getInGamePlayersByRole(Role.WOLF);
        inGameWolves.forEach(wolf -> {
            // reset wolf hasVoted field after wolf kill action
            wolf.setHasVoted(false);
        });


        log.info("Werewolf Action Completed");
        setStatus(STATUS.FINISHED);
        resetVoteForRole(playerService, Role.WOLF);
        voiceOutputService.speak(VoiceProperties.WOLF_CLOSE_EYES_ACTION_FILE_NAME);
        return true;
    }

}
