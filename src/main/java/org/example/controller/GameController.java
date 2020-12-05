package org.example.controller;

import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

/**
 * Created by daqwang on 9/2/20.
 */
@Controller
@RequestMapping("/game")
public class GameController {


    private final GameService gameService;


    @Autowired
    public GameController(final GameService gameService) {
        this.gameService = gameService;
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


    @GetMapping(value = "/ready/{userId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<StompPrincipal> ready(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(gameService.readyPlayer(userId));
    }


    @GetMapping(value = "/player/status", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<StompPrincipal>> playerStatus() {
        return ResponseEntity.ok(gameService.showPlayersStatus());
    }

}
