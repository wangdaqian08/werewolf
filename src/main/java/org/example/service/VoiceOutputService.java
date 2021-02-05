package org.example.service;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import lombok.extern.slf4j.Slf4j;
import org.example.config.VoiceProperties;
import org.example.model.GameResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.example.utils.VoiceResourceGenerator.stopCurrentThread;

/**
 * Created by daqwang on 18/12/20.
 */
@Component
@Slf4j
public class VoiceOutputService {

    private final BasicPlayer basicPlayer;

    @Autowired
    public VoiceOutputService(final BasicPlayer basicPlayer) {
        this.basicPlayer = basicPlayer;
    }

    public void speak(String mp3FileName) {
        try {
            playMp3(mp3FileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void announceWinner(GameResult gameResult) {
        if (GameService.VILLAGERS_WIN.equalsIgnoreCase(gameResult.getMessage())) {
            speak(VoiceProperties.VILLAGER_WIN_NIGHT_FILE_NAME);
        } else {
            speak(VoiceProperties.WOLF_WIN_NIGHT_FILE_NAME);
        }
    }

    private void playMp3(final String fileName) throws IOException {
        File audioFile = new ClassPathResource("/mp3" + File.separator + fileName).getFile();
        try {
            if (!audioFile.exists()) {
                throw new FileNotFoundException(fileName);
            }
            while (basicPlayer.getStatus() == BasicPlayer.PLAYING) {
                //make sure only 1 file plays at a time
                stopCurrentThread(500L);
            }
            log.info("playing file:{}", audioFile.getName());
            basicPlayer.open(audioFile);
            basicPlayer.play();
            log.info("voice played");
        } catch (BasicPlayerException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
