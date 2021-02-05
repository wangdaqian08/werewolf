package org.example.utils;

import com.google.cloud.texttospeech.v1beta1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.example.config.VoiceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by daqwang on 17/1/21.
 */
@Component
@Slf4j
public class VoiceResourceGenerator {

    private static final String MP3_FOLDER_NAME = "mp3";
    private final VoiceProperties voiceProperties;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Autowired
    public VoiceResourceGenerator(final VoiceProperties voiceProperties) {
        this.voiceProperties = voiceProperties;
    }


    public String generateVoiceFile(final String voiceMessageContent, final String mp3FileName) {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(voiceMessageContent).build();

            // Build the voice request, select the language code ("en-US") and the ssml voice gender
            // ("neutral")
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("cmn-CN")
                            .setName("cmn-CN-Wavenet-A")
                            .setSsmlGender(SsmlVoiceGender.FEMALE)
                            .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig =
                    AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();
            final String filePath = MP3_FOLDER_NAME + File.separator + mp3FileName;
            // Write the response to the output directory and file.
            OutputStream out = new FileOutputStream(filePath);
            out.write(audioContents.toByteArray());
            log.info("Audio content written to file \"{} \"", mp3FileName);
            return filePath;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public List<Voice> listAllSupportedVoices() throws Exception {
        // Instantiates a client
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Builds the text to speech list voices request
            ListVoicesRequest request = ListVoicesRequest.getDefaultInstance();

            // Performs the list voices request
            ListVoicesResponse response = textToSpeechClient.listVoices(request);
            List<Voice> voices = response.getVoicesList();

            for (Voice voice : voices) {
                // Display the voice's name. Example: tpc-vocoded
                System.out.format("Name: %s\n", voice.getName());

                // Display the supported language codes for this voice. Example: "en-us"
                List<ByteString> languageCodes = voice.getLanguageCodesList().asByteStringList();
                for (ByteString languageCode : languageCodes) {
                    System.out.format("Supported Language: %s\n", languageCode.toStringUtf8());
                }

                // Display the SSML Voice Gender
                System.out.format("SSML Voice Gender: %s\n", voice.getSsmlGender());

                // Display the natural sample rate hertz for this voice. Example: 24000
                System.out.format("Natural Sample Rate Hertz: %s\n\n", voice.getNaturalSampleRateHertz());
            }
            return voices;
        }
    }


    /**
     * create mp3 voice files with environment variable set:
     * GOOGLE_APPLICATION_CREDENTIALS=/Users/daqwang/Desktop/Werewolf-e728ddd7afbd.json
     */
    public void executeVoiceGenerationTasks() {
        List<Callable<String>> tasks = buildVoiceTasks();

        try {
            List<Future<String>> futures = executorService.invokeAll(tasks);

            AtomicInteger finishedTaskCounter = new AtomicInteger(0);
            AtomicInteger finishedTaskCount = new AtomicInteger((int) futures.stream().filter(Future::isDone).count());

            while (finishedTaskCount.get() != tasks.size()) {

                log.info("task status:{}/{}", finishedTaskCounter.getAcquire(), tasks.size());
                Thread.sleep(1000L);
                finishedTaskCount.getAndSet((int) futures.stream().filter(Future::isDone).count());
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void stopCurrentThread(long timeToStopInMs) {
        try {
            Thread.sleep(timeToStopInMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("exception in sleep,{}", e.getMessage(), e);
        }
    }

    private List<Callable<String>> buildVoiceTasks() {
        List<Callable<String>> mp3FileGenerateTaskList = new ArrayList<>();
        Arrays.stream(VoiceProperties.class.getDeclaredFields()).filter(field -> !"MP3_SUFFIX".equals(field.getName()) && !Modifier.isStatic(field.getModifiers())).forEach(field -> {

            String voiceResourceFileName = voiceProperties.getVoiceResourceFileName(field.getName());
            try {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), VoiceProperties.class);
                String voiceContent = (String) propertyDescriptor.getReadMethod().invoke(voiceProperties);
                log.info("field name:{}, content:{}, Mp3FileName:{}", field.getName(), voiceContent, voiceResourceFileName);
                Callable<String> fileTask = () -> generateVoiceFile(voiceContent, voiceResourceFileName);

                mp3FileGenerateTaskList.add(fileTask);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return mp3FileGenerateTaskList;
    }

}
