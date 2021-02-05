package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.schedule.Scheduler;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by daqwang on 9/2/20.
 */
@Slf4j
@Controller
@RequestMapping("/player")
public class PlayerController {


    private final PlayerService playerService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Scheduler scheduler;


    @Autowired
    public PlayerController(final PlayerService playerService, final Scheduler scheduler, final SimpMessagingTemplate simpMessagingTemplate) {

        this.playerService = playerService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.scheduler = scheduler;
    }


    /**
     * return list of players to vote, not include the requested player.
     *
     * @param name request StompPrincipal name
     * @return VoteList
     */
    @GetMapping(value = "/voteList/{userId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<StompPrincipal>> votePlayerListById(@PathVariable("userId") String name) {
        if (StringUtils.isBlank(name)) {
            log.error("Invalid user id, user id is empty");
            return ResponseEntity.status(400).body(Collections.emptyList());
        }
        List<StompPrincipal> voteList = playerService.getInGamePlayers().stream()
                .filter(player -> !player.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
        return ResponseEntity.ok(voteList);
    }

    /**
     * @param name StompPrincipal name
     * @return ResponseEntity<List < StompPrincipal>>
     */
    @GetMapping(value = "/details/{userId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<StompPrincipal>> playerStatusById(@PathVariable("userId") String name) {
        if (StringUtils.isBlank(name)) {
            log.error("Invalid user id, user id is empty");
            return ResponseEntity.status(400).body(Collections.emptyList());
        }
        List<StompPrincipal> playerByIdList = playerService.getReadyPlayerList().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
        return ResponseEntity.ok(playerByIdList);
    }

    /**
     * feature players(wolf,witch,seer) make this request. get list of <b>inGame</b> players to can be killed,saved,poisoned and checked.
     *
     * @param name playerName
     * @return list of users have different roles compare to the request player<br/>
     * list of inGame players to be killed,saved,poisoned and checked.
     */
    @GetMapping(value = "/forAction/{userId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<StompPrincipal>> getPlayersListForRole(@PathVariable("userId") String name) {
        if (StringUtils.isBlank(name)) {
            log.error("invalid user, user name is empty");
            return ResponseEntity.status(400).body(Collections.emptyList());
        }
        StompPrincipal validRequestFeaturePlayer = isValidateRequestPlayer(playerService.getInGamePlayers().stream().filter(player -> player.getName().equalsIgnoreCase(name)).findAny());
        if (validRequestFeaturePlayer == null) {
            log.error("can't find inGame feature user for name:{}", name);
            return ResponseEntity.status(400).body(Collections.emptyList());
        }
        List<StompPrincipal> playersWithDifferentRole = playerService.getInGamePlayers().stream()
                .filter(player -> !player.getRole().name().equalsIgnoreCase(validRequestFeaturePlayer.getRole().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(playersWithDifferentRole);
    }


    private StompPrincipal isValidateRequestPlayer(Optional<StompPrincipal> optionalStompPrincipal) {
        if (optionalStompPrincipal.isPresent()) {
            StompPrincipal stompPrincipal = optionalStompPrincipal.get();
            Optional<Role> roleOptional = List.of(Role.SEER, Role.WOLF, Role.WITCH)
                    .stream()
                    .filter(role -> role.name().equalsIgnoreCase(stompPrincipal.getRole().name()))
                    .findAny();
            if (roleOptional.isPresent()) {
                return stompPrincipal;
            }
        }
        return optionalStompPrincipal.orElse(null);
    }


}
