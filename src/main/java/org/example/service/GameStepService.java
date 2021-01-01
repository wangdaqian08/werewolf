package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.action.SeerAction;
import org.example.action.TransitAction;
import org.example.action.WerewolfAction;
import org.example.action.WitchAction;
import org.example.model.ActionResult;
import org.example.model.GameMessage;
import org.example.model.StompPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.example.utils.EndpointConstant.BROADCAST_DESTINATION;


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
    private final PlayerService playerService;
    private final VoiceOutputService voiceOutputService;
    private final WerewolfAction werewolfAction;
    private final WitchAction witchAction;
    private final SeerAction seerAction;
    private final LinkedList<Callable<Object>> gameSteps = new LinkedList<>();


    @Autowired
    public GameStepService(final PlayerService playerService, final WerewolfAction werewolfAction, final SeerAction seerAction, final WitchAction witchAction, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {

        this.simpMessagingTemplate = simpMessagingTemplate;
        this.voiceOutputService = voiceOutputService;

        this.playerService = playerService;
        this.werewolfAction = werewolfAction;
        this.witchAction = witchAction;
        this.seerAction = seerAction;

        initGameActions();
    }


    public void initGameActions() {
        ActionResult actionResult = ActionResult.getInstance("calculate victim");
        actionResult.reset();
        gameSteps.offer(createTransitTimeAction(CLOSE_EYES_ACTION_MESSAGE, 10));
        gameSteps.offer(werewolfAction);
        gameSteps.offer(createTransitTimeAction("start soon....", 5));
        gameSteps.offer(seerAction);
        gameSteps.offer(createTransitTimeAction("start soon....", 5));
        gameSteps.offer(witchAction);
        gameSteps.offer(createTransitTimeAction("role actions finished", 10));

    }

    private List<StompPrincipal> calculateVictims(ActionResult actionResult) {

        Map<StompPrincipal, GameService.RoleAction> victimMap = actionResult.getResultPlayer().entrySet().stream()
                .filter(entrySet -> entrySet.getValue().equals(GameService.RoleAction.KILL) || entrySet.getValue().equals(GameService.RoleAction.POISONING))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (CollectionUtils.isEmpty(victimMap)) {
            log.info("calculate victim,found no one killed at night");
        } else {
            Map<StompPrincipal, GameService.RoleAction> witchHelpedPlayer = victimMap.entrySet()
                    .stream()
                    .filter(entrySet -> entrySet.getValue().equals(GameService.RoleAction.SAVE))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            //set the witch helped player back to game
            if (!CollectionUtils.isEmpty(witchHelpedPlayer) && witchHelpedPlayer.size() == 1) {
                Map.Entry<StompPrincipal, GameService.RoleAction> witchHelpedActionEntry =
                        (Map.Entry<StompPrincipal, GameService.RoleAction>) witchHelpedPlayer.entrySet();
                if (victimMap.containsKey(witchHelpedActionEntry.getKey())) {
                    log.info("remove killed player from victim map, because saved by witch");
                    victimMap.remove(witchHelpedActionEntry.getKey());
                }
            }
            actionResult.reset();
        }
        // TODO 1/1/21
        // check the empty map keyset()
        victimMap.forEach((key, value) -> key.setInGame(false));
        return new ArrayList<>(victimMap.keySet());
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

    public void startGame() throws InterruptedException {
        //execute one by one, werewolvesActions execute first, when finished,execute witchActions, when withcActions finished execute seerActions

        try {
            List<Future<Object>> futures = executorService.invokeAll(gameSteps);
            List<Future<Object>> finished = new ArrayList<>();
            while (finished.size() != gameSteps.size()) {
                finished = futures.stream().filter(Future::isDone)
                        .collect(Collectors.toList());
                log.info("finished tasks:{}", finished.size());
                Thread.sleep(5000L);
            }
            gameSteps.clear();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            executorService.awaitTermination(5, TimeUnit.SECONDS);
            log.info("tasks shutdown:{}", executorService.isShutdown());
        }
        log.info("all actions are finished");


        // calculate victim report
        String weakUpMessage = createWeakUpMessage(calculateVictims(ActionResult.getInstance("calculate victims")));
        simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage(weakUpMessage));
    }

}
