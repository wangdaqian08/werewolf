package org.example.action;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Role;
import org.example.service.PlayerService;
import org.example.service.VoiceOutputService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.utils.EndpointConstant.PRIVATE_WITCH_ACTION_DESTINATION;

/**
 * Created by daqwang on 20/12/20.
 */
@Component
@Slf4j
public class WitchAction extends AbstractGameAction {


    public final static String POISON = "poison";
    public final static String ANTIDOTE = "antidote";
    private final static String WITCH_ACTION_MESSAGE = "Witch your have a poison and a antidote";
    private final static String WITCH_ACTION_CLOSE_EYES_MESSAGE = "Witch please close your eyes";
    private final Map<String, Boolean> witchItems = new HashMap<>();
    private final VoiceOutputService voiceOutputService;

    public WitchAction(final PlayerService playerService, final SimpMessagingTemplate simpMessagingTemplate, final VoiceOutputService voiceOutputService) {
        super(simpMessagingTemplate, playerService);
        this.voiceOutputService = voiceOutputService;
        this.setStatus(STATUS.READY);
        witchItems.put(POISON, true);
        witchItems.put(ANTIDOTE, true);
    }

    public boolean consumeWitchItem(final String item) {
        if (item.equalsIgnoreCase(ANTIDOTE) || item.equalsIgnoreCase(POISON)) {
            return witchItems.remove(item);
        } else {
            throw new RuntimeException("no matching witch items found for: " + item);
        }
    }

    public Map<String, Boolean> getAvailableWitchItems() {

        return this.witchItems.entrySet().stream()
                .filter(Map.Entry::getValue)
                .collect(Collectors.toMap(Map.Entry::getKey, stringBooleanEntry -> true));
    }

    @Override
    public Object call() {
        setStatus(STATUS.IN_PROGRESS);

        sendPrivateRoleMessageToPlayer(PRIVATE_WITCH_ACTION_DESTINATION, WITCH_ACTION_MESSAGE, Role.WITCH);
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
        log.info("Witch Action Completed");
        setStatus(STATUS.FINISHED);

        voiceOutputService.speak(WITCH_ACTION_CLOSE_EYES_MESSAGE);
        return true;
    }
}
