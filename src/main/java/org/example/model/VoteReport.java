package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by daqwang on 12/12/20.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteReport {

    @JsonIgnore
    private static VoteReport voteReport;
    private String message;
    private Boolean isDraw;
    private Boolean voteCompleted;
    private Map<StompPrincipal, Set<StompPrincipal>> details = new HashMap<>();
    private List<StompPrincipal> drawList;

    private VoteReport() {
    }

    public static VoteReport getInstance() {
        if (voteReport == null) {
            voteReport = new VoteReport();
        }
        return voteReport;
    }

    /**
     * vote details, for example:
     * Jack --> Lucy, Mark, Jason <br/>
     * Jack been voted by 3 players: Lucy, Mark, Jason.
     *
     * @param player      the player
     * @param voteDetails the players who vote this  player
     */
    public void makeVoteDetails(final StompPrincipal player, final Set<StompPrincipal> voteDetails) {
        details.put(player, voteDetails);
    }


}
