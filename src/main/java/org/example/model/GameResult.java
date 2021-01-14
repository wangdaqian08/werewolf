package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by daqwang on 9/1/21.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class GameResult {
    private boolean isFinished;
    private String message;


    public GameResult(boolean isFinished, String message) {
        this.isFinished = isFinished;
        this.message = message;
    }
}
