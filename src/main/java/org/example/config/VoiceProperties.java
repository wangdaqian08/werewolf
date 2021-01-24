package org.example.config;

import lombok.Data;
import lombok.ToString;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author daqwang
 * @date 17/1/21
 */
@ToString
@Data
public class VoiceProperties {

    public static final String MP3_SUFFIX = ".mp3";
    public static final String SKY_DARK_FILE_NAME = "skyDark.mp3";
    public static final String WOLF_ACTION_FILE_NAME = "wolfAction.mp3";
    public static final String WOLF_CLOSE_EYES_ACTION_FILE_NAME = "wolfCloseEyesAction.mp3";
    public static final String WITCH_ACTION_FILE_NAME = "witchAction.mp3";
    public static final String WITCH_CLOSE_EYES_ACTION_FILE_NAME = "witchCloseEyesAction.mp3";
    public static final String SEER_ACTION_FILE_NAME = "seerAction.mp3";
    public static final String SEER_CLOSE_EYES_ACTION_FILE_NAME = "seerCloseEyesAction.mp3";
    public static final String SAFE_NIGHT_FILE_NAME = "safeNight.mp3";
    public static final String VICTIM_NIGHT_FILE_NAME = "victimNight.mp3";
    public static final String WOLF_WIN_NIGHT_FILE_NAME = "wolfWin.mp3";
    public static final String VILLAGER_WIN_NIGHT_FILE_NAME = "villagerWin.mp3";
    public static final String GAME_CONTINUE_FILE_NAME = "gameContinue.mp3";
    public static final String GAME_VOTE_FILE_NAME = "gameVote.mp3";
    private String skyDark;
    private String wolfAction;
    private String wolfCloseEyesAction;
    private String witchAction;
    private String witchCloseEyesAction;
    private String seerAction;
    private String seerCloseEyesAction;
    private String safeNight;
    private String victimNight;
    private String wolfWin;
    private String villagerWin;
    private String gameContinue;
    private String gameVote;

    public String getVoiceResourceFileName(final String voiceResource) {
        Field[] declaredFields = this.getClass().getDeclaredFields();
        if (Arrays.stream(declaredFields).anyMatch(field -> field.getName().equals(voiceResource))) {
            return voiceResource + MP3_SUFFIX;
        }
        throw new RuntimeException("Field not found: " + voiceResource);
    }
}
