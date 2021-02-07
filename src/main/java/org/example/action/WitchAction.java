package org.example.action;

import lombok.extern.slf4j.Slf4j;
import org.example.config.VoiceProperties;
import org.example.model.ActionResult;
import org.example.model.ExecuteAction;
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
    private final static String WITCH_ACTION_MESSAGE_TEMPLATE = "Witch, you have %s";
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
        voiceOutputService.speak(VoiceProperties.WITCH_ACTION_FILE_NAME);
        String witchActionMessage = createAvailableItemsString(generateAvailableWitchItems());
        sendPrivateRoleMessageToPlayer(PRIVATE_WITCH_ACTION_DESTINATION, witchActionMessage, Role.WITCH);
        // TODO 20/12/20
        // voiceOutputService.speak(SEER_ACTION_MESSAGE)
        log.info("WITCH Action Started");
        while (!isActionCompleted(Role.WITCH)) {
            // block the thread if this action haven't completed
            try {
                Thread.sleep(8000L);
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
        voiceOutputService.speak(VoiceProperties.WITCH_CLOSE_EYES_ACTION_FILE_NAME);
        return true;
    }

    public List<String> generateAvailableWitchItems() {
        return this.witchItems.entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String createAvailableItemsString(final List<String> availableItems){
        String saveMessage = "";
        ActionResult instance = ActionResult.getInstance("");

        ExecuteAction killedPlayerAction = instance.getActionedPlayerList().stream().filter(executeAction -> executeAction.getRoleAction().equals(KILL)).findFirst().orElse(null);
        if (killedPlayerAction != null) {

            String killedPlayerNickName = killedPlayerAction.getPlayer().getNickName();
            saveMessage = " This player " + killedPlayerNickName + " is killed, do you want to save? ";
        }
        if (CollectionUtils.isEmpty(availableItems)) {
            return "You don't have any available item";
        } else {
            return String.format(WitchAction.WITCH_ACTION_MESSAGE_TEMPLATE, String.join(",", availableItems)) + saveMessage;
        }
    }
}
