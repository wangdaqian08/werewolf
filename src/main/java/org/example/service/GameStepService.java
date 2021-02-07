package org.example.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.example.action.SeerAction;
import org.example.action.TransitAction;
import org.example.action.WerewolfAction;
import org.example.action.WitchAction;
import org.example.config.VoiceProperties;
import org.example.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.example.service.GameService.RoleAction.*;
import static org.example.utils.EndpointConstant.BROADCAST_DESTINATION;
import static org.example.utils.EndpointConstant.BROADCAST_PLAYER_STATUS_DESTINATION;


/**
 * Created by daqwang on 13/12/20.
 */
@Service
@Slf4j
public class GameStepService {

    private final static String CLOSE_EYES_ACTION_MESSAGE = "The sky goes dark, players close your eyes";
    private final static String SAFE_ACTION_MESSAGE = "no one got killed";
    private final static String WAKE_UP_ACTION_BASE_MESSAGE = "It's dawned. Last night ";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PlayerService playerService;
    private final VoiceOutputService voiceOutputService;
    private final WerewolfAction werewolfAction;
    private final WitchAction witchAction;
    private final SeerAction seerAction;
    private final GameService gameService;
    private final LinkedList<Callable<Object>> gameSteps = new LinkedList<>();
    private final AtomicBoolean isGameStarted = new AtomicBoolean(false);

    @Autowired
    public GameStepService(final GameService gameService, final PlayerService playerService, final WerewolfAction werewolfAction, final SeerAction seerAction, final WitchAction witchAction, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {

        this.simpMessagingTemplate = simpMessagingTemplate;
        this.voiceOutputService = voiceOutputService;

        this.gameService = gameService;
        this.playerService = playerService;
        this.werewolfAction = werewolfAction;
        this.witchAction = witchAction;
        this.seerAction = seerAction;

        initGameActions();
    }

    public AtomicBoolean getIsGameStarted() {
        return isGameStarted;
    }


    public void initGameActions() {
        ActionResult actionResult = ActionResult.getInstance("calculate victim");
        actionResult.reset();
        gameSteps.offer(createTransitTimeAction(VoiceProperties.SKY_DARK_FILE_NAME, 3));
        gameSteps.offer(werewolfAction);
        gameSteps.offer(createTransitTimeAction("start soon....", 5));
        gameSteps.offer(seerAction);
        gameSteps.offer(createTransitTimeAction("start soon....", 5));
        gameSteps.offer(witchAction);
        gameSteps.offer(createTransitTimeAction("role actions finished", 1));

    }


    private List<StompPrincipal> calculateVictims(ActionResult actionResult) {
        List<StompPrincipal> victimList = actionResult.getActionedPlayerList().stream()
                .filter(executeAction -> executeAction.getRoleAction() == KILL || executeAction.getRoleAction() == POISONING)
                .map(ExecuteAction::getPlayer)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(victimList)) {
            log.info("calculate victim,found no one killed at night");
        } else {
            List<StompPrincipal> witchHelpedPlayers = actionResult.getActionedPlayerList().stream()
                    .filter(executeAction -> executeAction.getRoleAction() == SAVE)
                    .map(ExecuteAction::getPlayer)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(witchHelpedPlayers) && witchHelpedPlayers.size() > 1) {
                throw new RuntimeException("Witch can only help 1 play");
            }
            //set the witch helped player back to game
            if (!CollectionUtils.isEmpty(witchHelpedPlayers)) {
                StompPrincipal helpedStompPrincipal = witchHelpedPlayers.get(0);
                helpedStompPrincipal.setInGame(true);
                // remove helped player from victim list

                boolean removed = victimList.remove(helpedStompPrincipal);
                if(removed){
                    log.info("remove killed player from victim map, because saved by witch");
                }
            }
            actionResult.reset();
        }

        victimList.forEach(victim->{
            victim.setInGame(false);
        });
        return victimList.stream().distinct().collect(Collectors.toList());
    }

    private String createWeakUpMessage(List<StompPrincipal> victims) {
        if (CollectionUtils.isEmpty(victims)) {
            voiceOutputService.speak(VoiceProperties.SAFE_NIGHT_FILE_NAME);
            return WAKE_UP_ACTION_BASE_MESSAGE + SAFE_ACTION_MESSAGE;
        } else {
            String victimNames = victims.stream().map(StompPrincipal::getNickName).collect(Collectors.joining(","));
            voiceOutputService.speak(VoiceProperties.VICTIM_NIGHT_FILE_NAME);
            return WAKE_UP_ACTION_BASE_MESSAGE + victimNames + " dead";
        }
    }

    private TransitAction createTransitTimeAction(String transitActionMessage, int secondsToWait) {
        return new TransitAction(TimeUnit.MILLISECONDS.convert(secondsToWait, TimeUnit.SECONDS), transitActionMessage, this.voiceOutputService);

    }

    public void startGame() throws InterruptedException {
        //execute one by one, werewolvesActions execute first, when finished,execute witchActions, when witchActions finished execute seerActions

        try {
            if(isGameStarted.get()){
                return;
            }
            List<Future<Object>> futures = executorService.invokeAll(gameSteps);
            isGameStarted.getAndSet(true);
            List<Future<Object>> finished = new ArrayList<>();
            while (finished.size() != gameSteps.size()) {
                finished = futures.stream().filter(Future::isDone)
                        .collect(Collectors.toList());
                log.info("finished tasks:{}", finished.size());
                Thread.sleep(100L);
            }
            gameSteps.clear();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        finally {
//            executorService.awaitTermination(15, TimeUnit.MINUTES);
//            log.info("tasks shutdown:{}", executorService.isShutdown());
//        }
        log.info("all actions are finished");


        // calculate victim report
        String weakUpMessage = createWeakUpMessage(calculateVictims(ActionResult.getInstance("calculate victims")));
        simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage(weakUpMessage));

        GameResult gameResult = gameService.isGameFinished(playerService.getInGamePlayers());
        if (gameResult.isFinished()) {
            //game finished
            voiceOutputService.announceWinner(gameResult);
            simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage(gameResult.getMessage()));

        } else {
            // reset ready player vote status

            // TODO 24/1/21 speak()  start new Thread need to find a way make sure the previous method finished until next one will call
            //
            voiceOutputService.speak(VoiceProperties.GAME_CONTINUE_FILE_NAME);
            List<StompPrincipal> stompPrincipals = playerService.resetVoteCount();
            simpMessagingTemplate.convertAndSend(BROADCAST_PLAYER_STATUS_DESTINATION, new Gson().toJson(stompPrincipals));
            voiceOutputService.speak(VoiceProperties.GAME_VOTE_FILE_NAME);
            simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage("Start Voting...."));
            isGameStarted.getAndSet(false);
        }
    }

}
