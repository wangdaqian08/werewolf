package org.example.controller;

import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by daqwang on 9/2/20.
 */
@Controller
@RequestMapping("/game")
public class GameController {


    private final GameService gameService;
    private final PlayerService playerService;


    @Autowired
    public GameController(final GameService gameService, final PlayerService playerService) {
        this.gameService = gameService;
        this.playerService = playerService;
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
    public ResponseEntity<StompPrincipal> ready(@PathVariable("userId") String userId, @PathVariable("nickname") String nickname) {
        return ResponseEntity.ok(gameService.readyPlayer(userId, nickname));
    }


    @GetMapping(value = "/player/status", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<StompPrincipal>> playerStatus() {
        return ResponseEntity.ok(playerService.showPlayersStatus());
    }


}
