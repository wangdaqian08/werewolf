package org.example.service;

import org.example.action.SeerAction;
import org.example.action.WerewolfAction;
import org.example.action.WitchAction;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daqwang on 6/12/20.
 */
@RunWith(MockitoJUnitRunner.class)
public class GameStepServiceTest {

    private PlayerService playerService;
    private VoteService voteService;

    private GameService gameService;

    private GameStepService gameStepService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;


    @Before
    public void setup() {

        this.playerService = new PlayerService();
        VoiceOutputService voiceOutputService = new VoiceOutputService();
        SeerAction seerAction = new SeerAction(playerService, simpMessagingTemplate, voiceOutputService);
        WerewolfAction werewolfAction = new WerewolfAction(playerService, simpMessagingTemplate, voiceOutputService);
        WitchAction witchAction = new WitchAction(playerService, simpMessagingTemplate, voiceOutputService);
        this.voteService = new VoteService(playerService, witchAction);
        this.gameStepService = new GameStepService(playerService, werewolfAction, seerAction, witchAction, simpMessagingTemplate, voiceOutputService);
        this.gameService = new GameService(playerService, voteService, simpMessagingTemplate);

    }

    @Test
    public void testGameSteps() throws InterruptedException {
        gameStepService.startGame();
    }


    @Test
    public void testDeal() {
        List<StompPrincipal> players = createPlayers(7, "testNickname");

        GameService gameService = new GameService(playerService, voteService, simpMessagingTemplate);
        players.forEach(player -> {
            gameService.readyPlayer(player.getName(), "testNickname");
        });
        List<StompPrincipal> playersWithRole = gameService.deal();
        long count = playersWithRole.stream().filter(player -> player.getRole().equals(Role.VILLAGER)).count();
        // total 7 players should have 3 villagers
        Assert.assertEquals(3, count);
    }


    public List<StompPrincipal> createPlayers(int size, String nickname) {
        List<StompPrincipal> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(playerService.createPlayer(String.valueOf(System.currentTimeMillis())));
        }
        list.forEach(player -> {
            player.setNickName(nickname);
        });
        return list;
    }
}
