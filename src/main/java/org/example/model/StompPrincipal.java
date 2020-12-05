package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.Principal;

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
    private boolean isReady;
    private Role role;

    public StompPrincipal(String name) {
        this.name = name;
    }

    public StompPrincipal(String name,String sessionId) {
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
}
