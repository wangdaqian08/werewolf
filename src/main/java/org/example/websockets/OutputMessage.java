package org.example.websockets;

import lombok.Data;

@Data
public class OutputMessage {

    private String from;
    private String text;
    private String time;
    private String name;

    public OutputMessage(final String from, final String text, final String time) {

        this.from = from;
        this.text = text;
        this.time = time;
    }
    public OutputMessage(final String from, final String text, final String time,final String name) {

        this.from = from;
        this.text = text;
        this.time = time;
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public String getFrom() {
        return from;
    }
}
