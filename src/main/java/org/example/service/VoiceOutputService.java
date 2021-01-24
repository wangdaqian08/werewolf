package org.example.service;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Created by daqwang on 18/12/20.
 */
@Component
@Slf4j
public class VoiceOutputService {

    public String message;

    public VoiceOutputService() {
    }

    public void outputVoicMessage(String message) {

    }

    public void speak(String mp3FileName) {
        try {
            playMp3(mp3FileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playMp3(final String fileName) throws IOException {

        File audioFile = new ClassPathResource("/mp3" + File.separator + fileName).getFile();

        if (!audioFile.exists()) {
            throw new FileNotFoundException(fileName);
        }

        BasicPlayer player = new BasicPlayer();
        try {
            player.open(new URL("file:///" + audioFile.getAbsolutePath()));
            player.play();
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }
}
