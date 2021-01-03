package org.example.service;

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
public class GameServiceTest {

    private PlayerService playerService;
    private VoteService voteService;


    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private VoiceOutputService voiceOutputService;


    @Before
    public void setup() {

        this.playerService = new PlayerService();
        WitchAction witchAction = new WitchAction(playerService, simpMessagingTemplate, voiceOutputService);
        this.voteService = new VoteService(this.playerService, witchAction, simpMessagingTemplate);

    }

    @Test
    public void testCreatePlayers() {
        List<StompPrincipal> players = createPlayers(7, "testNickname");
        Assert.assertEquals(players.size(), 7);
        System.out.println(players);
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
