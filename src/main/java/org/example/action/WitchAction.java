package org.example.action;

import lombok.extern.slf4j.Slf4j;
import org.example.model.ActionResult;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.service.PlayerService;
import org.example.service.VoiceOutputService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.service.GameService.RoleAction.KILL;
import static org.example.utils.EndpointConstant.PRIVATE_WITCH_ACTION_DESTINATION;

/**
 * Created by daqwang on 20/12/20.
 */
@Component
@Slf4j
public class WitchAction extends AbstractGameAction {


    public final static String POISON = "poison";
    public final static String ANTIDOTE = "antidote";
    private final static String WITCH_ACTION_MESSAGE_TEMPLATE = "WItch, you have %s";
    private final static String WITCH_ACTION_CLOSE_EYES_MESSAGE = "Witch please close your eyes";
    private final Map<String, Boolean> witchItems = new HashMap<>();
    private final VoiceOutputService voiceOutputService;

    public WitchAction(final PlayerService playerService, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {
        super(playerService, simpMessagingTemplate);
        this.voiceOutputService = voiceOutputService;
        this.setStatus(STATUS.READY);
        witchItems.put(POISON, true);
        witchItems.put(ANTIDOTE, true);
    }

    private boolean consumeWitchItem(final String item) {
        if (item.equalsIgnoreCase(ANTIDOTE) || item.equalsIgnoreCase(POISON)) {
            return witchItems.remove(item);
        } else {
            throw new RuntimeException("no matching witch items found for: " + item);
        }
    }

    public boolean consumeAvailableWitchItem(final String witchItem) {

        return this.witchItems.entrySet().stream()
                .filter(witchItemEntry -> witchItemEntry.getValue() && witchItemEntry.getKey().equalsIgnoreCase(witchItem))
                .findAny()
                .map(witchItemEntry -> consumeWitchItem(witchItemEntry.getKey()))
                .orElse(false);
    }

    @Override
    public Object call() {
        setStatus(STATUS.IN_PROGRESS);

        String witchActionMessage = generateAvailableWitchItems(witchItems);
        sendPrivateRoleMessageToPlayer(PRIVATE_WITCH_ACTION_DESTINATION, witchActionMessage, Role.WITCH);
        // TODO 20/12/20
        // voiceOutputService.speak(SEER_ACTION_MESSAGE)
        log.info("WITCH Action Started");
        while (!isActionCompleted(Role.WITCH)) {
            // block the thread if this action haven't completed
            try {
                Thread.sleep(800L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<StompPrincipal> inGameWitch = playerService.getInGamePlayersByRole(Role.WITCH);
        inGameWitch.forEach(witch -> {
            // reset witch hasVoted field after wolf kill action
            witch.setHasVoted(false);
        });
        log.info("Witch Action Completed");
        setStatus(STATUS.FINISHED);
        resetVoteForRole(playerService, Role.WITCH);
        voiceOutputService.speak(WITCH_ACTION_CLOSE_EYES_MESSAGE);
        return true;
    }

    private String generateAvailableWitchItems(final Map<String, Boolean> witchItems) {
        List<String> availableItems = witchItems.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
        ActionResult instance = ActionResult.getInstance("");
        StompPrincipal killedPlayer = instance.getResultPlayer().entrySet().stream().filter(entry -> entry.getValue().equals(KILL)).findFirst().map(Map.Entry::getKey).orElse(null);

        String saveMessage = "";
        if (killedPlayer != null) {
            saveMessage = " This player " + killedPlayer.getNickName() + " is killed, do you want to save? ";
        }
        if (CollectionUtils.isEmpty(availableItems)) {
            return "You don't have any available item";
        } else {
            return String.format(WitchAction.WITCH_ACTION_MESSAGE_TEMPLATE, String.join(",", availableItems)) + saveMessage;
        }
    }
}
