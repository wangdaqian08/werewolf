package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by daqwang on 24/11/20.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StompPrincipal implements Principal {

    private String name;
    private String sessionId;
    private boolean isReady = false;
    private Role role;
    private boolean inGame = false;
    private String nickName;
    private Integer voteCount = 0;
    private boolean hasVoted = false;
    private Set<StompPrincipal> votedBySet = new HashSet<>();

    public StompPrincipal(String name) {
        this.name = name;
        this.setInGame(true);
    }

    public StompPrincipal(String name, String sessionId) {
        this.name = name;
        this.sessionId = sessionId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StompPrincipal)) {
            return false;
        }

        StompPrincipal that = (StompPrincipal) o;

        return new EqualsBuilder()
                .append(name, that.name)
                .append(sessionId, that.sessionId)
                .append(role, that.role)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(sessionId)
                .append(role)
                .toHashCode();
    }

    public void voteBy(StompPrincipal voter) {
        this.votedBySet.add(voter);
        //increase vote count
        this.setVoteCount(this.getVoteCount() + 1);
    }
}
