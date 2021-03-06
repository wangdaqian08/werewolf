package org.example.action;

import lombok.extern.slf4j.Slf4j;
import org.example.config.VoiceProperties;
import org.example.service.VoiceOutputService;

import java.util.concurrent.Callable;

/**
 * Created by daqwang on 20/12/20.
 */

@Slf4j
public class TransitAction implements Callable<Object> {


    private final VoiceOutputService voiceOutputService;
    private final String message;
    private final Long timeToWaitInMilliseconds;


    public TransitAction(final Long timeToWaitInMilliseconds, final String message, final VoiceOutputService voiceOutputService) {

        this.timeToWaitInMilliseconds = timeToWaitInMilliseconds;
        this.message = message;
        this.voiceOutputService = voiceOutputService;
    }


    @Override
    public Object call() {
        if (message.contains(VoiceProperties.MP3_SUFFIX)) {
            log.info("speak message:{}", message);
            voiceOutputService.speak(message);
        }
        try {
            log.info("start transit action, message:{}", message);
            Thread.sleep(this.timeToWaitInMilliseconds);
            log.info("completed transit action");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
