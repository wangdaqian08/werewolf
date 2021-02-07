package org.example.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.service.GameService;
import org.thymeleaf.util.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by daqwang on 24/12/20.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ActionResult {

    private static ActionResult actionResult;
    private String resultMessage;
    // TODO 6/2/21 this need to refactor to an object
    @JsonUnwrapped
    private List<ExecuteAction> actionedPlayerList;

    public static ActionResult getInstance(final String message) {
        if (actionResult == null) {
            actionResult = new ActionResult(message);
            actionResult.actionedPlayerList = new ArrayList<>();
        } else {
            actionResult.setResultMessage(message);
        }
        return actionResult;

    }

    public ActionResult(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public void reset() {
        actionResult.setResultMessage("");
        actionResult.setActionedPlayerList(new ArrayList<>());
    }

    public List<ExecuteAction> findActionResultByActions(List<GameService.RoleAction> actions) {
        if (ListUtils.isEmpty(actions)) {
            log.warn("Actions are empty {}", actions);
            return Collections.emptyList();
        }
        if (ListUtils.isEmpty(this.getActionedPlayerList())) {
            return Collections.emptyList();
        }

        return this.getActionedPlayerList().stream()
                .filter(executeAction -> actions.contains(executeAction.getRoleAction()))
                .collect(Collectors.toList());
    }
}
