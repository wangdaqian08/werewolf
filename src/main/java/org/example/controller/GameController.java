package org.example.controller;

import com.google.gson.Gson;
import org.example.model.StompPrincipal;
import org.example.schedule.Scheduler;
import org.example.service.GameService;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.example.utils.EndpointConstant.BROADCAST_PLAYER_STATUS_DESTINATION;

/**
 * Created by daqwang on 9/2/20.
 */
@Controller
@RequestMapping("/game")
public class GameController {


    private final GameService gameService;
    private final PlayerService playerService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Scheduler scheduler;


    @Autowired
    public GameController(final GameService gameService, final PlayerService playerService, final Scheduler scheduler, final SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.playerService = playerService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.scheduler = scheduler;
    }

    /**
     * show index.html page in templates folder
     *
     * @param modelAndView
     * @return ModelAndView index.html
     */
    @RequestMapping("/index")
    public ModelAndView index(ModelAndView modelAndView) {
        modelAndView.setViewName("index");
        return modelAndView;
    }

    /**
     * test
     * show test.html page in templates folder
     *
     * @param modelAndView
     * @return test.html
     */
    @RequestMapping("/test")
    public ModelAndView test(ModelAndView modelAndView) {
        modelAndView.setViewName("test");
        return modelAndView;
    }


    @GetMapping(value = "/ready/{nickname}/{userId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<StompPrincipal> ready(@PathVariable("userId") String userId, @PathVariable("nickname") String nickname) throws InterruptedException {
        StompPrincipal stompPrincipalReady = gameService.readyPlayer(userId, nickname);
        List<StompPrincipal> stompPrincipals = playerService.showPlayersStatus();
        simpMessagingTemplate.convertAndSend(BROADCAST_PLAYER_STATUS_DESTINATION, new Gson().toJson(stompPrincipals));
        //aggressively start the game after the this request, instead of waiting for scheduled task.
        if (playerService.getReadyPlayerList().size() == playerService.getTotalPlayers()) {
            scheduler.assignRoles();
        }
        return ResponseEntity.ok(stompPrincipalReady);
    }


    @GetMapping(value = "/player/status", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<StompPrincipal>> playerStatus() {
        List<StompPrincipal> stompPrincipals = playerService.showPlayersStatus();
        return ResponseEntity.ok(playerService.showPlayersStatus());
    }

    /**
     * todo this method should be triggered, once the vote are finished
     *
     * @return
     */
    @GetMapping(value = "/startGame", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> startGame() throws InterruptedException {
        scheduler.startGameAgain();
        return ResponseEntity.ok("game started");
    }


}
