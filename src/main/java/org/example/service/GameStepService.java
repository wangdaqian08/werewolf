package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.action.SeerAction;
import org.example.action.TransitAction;
import org.example.action.WerewolfAction;
import org.example.action.WitchAction;
import org.example.model.StompPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Created by daqwang on 13/12/20.
 */
@Service
@Slf4j
public class GameStepService {

    private final static String CLOSE_EYES_ACTION_MESSAGE = "The sky goes dark, players close your eyes";
    private final static String SAFE_ACTION_MESSAGE = "Last night no got killed";
    private final static String WAKE_UP_ACTION_BASE_MESSAGE = "It's dawned. ";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final VoiceOutputService voiceOutputService;
    private final WerewolfAction werewolfAction;
    private final WitchAction witchAction;
    private final SeerAction seerAction;
    private final LinkedList<Callable<Object>> gameSteps = new LinkedList<>();
    private List<StompPrincipal> victims = new ArrayList<>();


    @Autowired
    public GameStepService(final WerewolfAction werewolfAction, final SeerAction seerAction, final WitchAction witchAction, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {

        this.simpMessagingTemplate = simpMessagingTemplate;
        this.voiceOutputService = voiceOutputService;

        this.werewolfAction = werewolfAction;
        this.witchAction = witchAction;
        this.seerAction = seerAction;

        initGameActions();
    }


    private void initGameActions() {
        gameSteps.offer(createTransitTimeAction(CLOSE_EYES_ACTION_MESSAGE, 10));
        gameSteps.offer(werewolfAction);
        gameSteps.offer(createTransitTimeAction("", 5));
        gameSteps.offer(seerAction);
        gameSteps.offer(createTransitTimeAction("", 5));
        gameSteps.offer(witchAction);
        gameSteps.offer(createTransitTimeAction(createWeakUpMessage(victims), 0));


    }

    private String createWeakUpMessage(List<StompPrincipal> victims) {
        if (CollectionUtils.isEmpty(victims)) {
            return WAKE_UP_ACTION_BASE_MESSAGE + SAFE_ACTION_MESSAGE;
        } else {
            String victimNames = victims.stream().map(StompPrincipal::getNickName).collect(Collectors.joining(","));
            return WAKE_UP_ACTION_BASE_MESSAGE + victimNames + " dead";
        }
    }

    private TransitAction createTransitTimeAction(String transitActionMessage, int secondsToWait) {
        return new TransitAction(TimeUnit.MILLISECONDS.convert(secondsToWait, TimeUnit.SECONDS), transitActionMessage, this.voiceOutputService);

    }

    public void startGame() {
        //execute one by one, werewolvesActions execute first, when finished,execute witchActions, when withcActions finished execute seerActions

        try {
            executorService.invokeAll(gameSteps);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
        log.info("all actions are finished");

        // TODO 24/12/20
        // calculate vote report/ result
        // reset victims, update in game status, reset witch action player (poison/antidote)
    }

}
