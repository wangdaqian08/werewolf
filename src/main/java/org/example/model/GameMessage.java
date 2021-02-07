package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Created by daqwang on 13/12/20.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class GameMessage {

    private String sender;
    protected String time;
    protected String message;
    private List<StompPrincipal> candidatePlayers;

    public GameMessage(String message) {
        this.message = message;
        Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        this.time = new SimpleDateFormat("HH:mm:ss").format(now);
        this.sender = "System:";
    }
}
