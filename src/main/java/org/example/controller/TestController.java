package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.config.VoiceProperties;
import org.example.model.StompPrincipal;
import org.example.schedule.Scheduler;
import org.example.service.GameService;
import org.example.service.PlayerService;
import org.example.service.VoiceOutputService;
import org.example.utils.VoiceResourceGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.example.config.VoiceProperties.MP3_SUFFIX;

/**
 * Created by daqwang on 9/2/20.
 */
@Controller
@Slf4j
@PropertySource("classpath:application.properties")
@RequestMapping("/test")
public class TestController {


    private final GameService gameService;
    private final PlayerService playerService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Scheduler scheduler;
    private final VoiceResourceGenerator voiceResourceGenerator;
    private final VoiceOutputService voiceOutputService;
    private VoiceProperties voiceProperties;

    @Autowired
    public TestController(final VoiceProperties voiceProperties, final VoiceResourceGenerator voiceResourceGenerator, final VoiceOutputService voiceOutputService, final GameService gameService, final PlayerService playerService, final Scheduler scheduler, final SimpMessagingTemplate simpMessagingTemplate) {
        this.voiceProperties = voiceProperties;
        this.gameService = gameService;
        this.playerService = playerService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.scheduler = scheduler;
        this.voiceOutputService = voiceOutputService;
        this.voiceResourceGenerator = voiceResourceGenerator;
    }

    /**
     * show index.html page in templates folder
     *
     * @return ModelAndView index.html
     */
    @RequestMapping("/voice")
    public ResponseEntity<String> voice() {
        String message = "天黑请闭眼！";
        voiceResourceGenerator.generateVoiceFile(message, "filename");
        return ResponseEntity.ok("ok");
    }

    @RequestMapping("/speak")
    public ResponseEntity<String> speak() {

        voiceOutputService.speak(VoiceProperties.GAME_CONTINUE_FILE_NAME);
        return ResponseEntity.ok("ok speak");
    }

    @RequestMapping("/generate/allVoices")
    public ResponseEntity<String> generateVoices() {

//        voiceResourceGenerator.executeVoiceGenerationTasks();
        return ResponseEntity.ok("start tasks...");
    }

    /**
     * for example: curl localhost:8080/test/generate/voices/safeNight
     *
     * @param fileName
     * @return path of the file
     */
    @RequestMapping("/generate/voices/{fileName}")
    public ResponseEntity<String> generateVoices(@PathVariable("fileName") String fileName) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        Field[] declaredFields = VoiceProperties.class.getDeclaredFields();
        List<Field> nonStaticPrivateFields = new ArrayList<>();
        for (Field field : declaredFields) {
            if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPrivate(field.getModifiers())) {
                nonStaticPrivateFields.add(field);
            }
        }
        Optional<Field> optionalField = nonStaticPrivateFields.stream().filter(staticField -> staticField.getName().equalsIgnoreCase(fileName)).findFirst();
        if (optionalField.isPresent()) {
            Field voiceFileField = optionalField.get();
            String voiceFileName = voiceProperties.getVoiceResourceFileName(voiceFileField.getName());
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(voiceFileField.getName(), VoiceProperties.class);
            String voiceContent = (String) propertyDescriptor.getReadMethod().invoke(voiceProperties);
            String filePath = voiceResourceGenerator.generateVoiceFile(voiceContent, voiceFileName);
            return ResponseEntity.ok("check file at," + filePath);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("can't find file, " + fileName);
        }
    }


    @RequestMapping("/remove/player/{id}")
    public ResponseEntity<String> removePlayerById(@PathVariable("id")String id) {
        List<StompPrincipal> players = playerService.getPlayers();
        Optional<StompPrincipal> optionalStompPrincipal = players.stream().filter(player -> player.getName().equals(id.trim())).findFirst();
        optionalStompPrincipal.ifPresent(players::remove);
        return ResponseEntity.ok("player: "+id+" removed");
    }


}
