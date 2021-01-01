package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.example.service.GameService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by daqwang on 24/12/20.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionResult {

    private static ActionResult actionResult;
    private String result;
    private Map<StompPrincipal, GameService.RoleAction> resultPlayer = new HashMap<>();

    public static ActionResult getInstance(final String message) {
        if (actionResult == null) {
            actionResult = new ActionResult(message);
        } else {
            actionResult.setResult(message);
        }
        return actionResult;

    }

    public ActionResult(String result) {
        this.result = result;
    }

    public void reset() {
        actionResult.setResult("");
        actionResult.setResultPlayer(new HashMap<>());
    }
}
