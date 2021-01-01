package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.ActionResult;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.example.service.GameStepService;
import org.example.service.PlayerService;
import org.example.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.example.service.GameService.RoleAction.*;

/**
 * Created by daqwang on 14/12/20.
 */
@Slf4j
@Controller
@RequestMapping("/vote")
public class VoteController {


    public final GameStepService gameStepService;
    public final VoteService voteService;
    public final PlayerService playerService;
    public final GameService gameService;

    @Autowired
    public VoteController(final GameStepService gameStepService, final VoteService voteService, final PlayerService playerService, final GameService gameService) {
        this.gameStepService = gameStepService;
        this.voteService = voteService;
        this.playerService = playerService;
        this.gameService = gameService;
    }

    /**
     * todo need to update to session ID replace nickname
     *
     * @param action
     * @param voterNickname
     * @param nickName
     * @return
     */
    @GetMapping("/action/{action}/voter/{voterNickname}/player/{nickName}")
    public ResponseEntity<ActionResult> vote(@PathVariable("action") String action, @PathVariable("voterNickname") String voterNickname, @PathVariable("nickName") String nickName) {
        StompPrincipal voter = playerService.getPlayerByNickName(voterNickname);
        GameService.RoleAction playerAction = GameService.RoleAction.valueOf(action.trim().toUpperCase());
        //check if player has the right permission to perform the action
        if (!isActionAuthorised(voter, playerAction)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ActionResult("player can't perform this action:" + action));
        }
        ActionResult actionResult = voteService.handleRoleAction(voter, nickName, playerAction);
        return ResponseEntity.ok(actionResult);
    }

    /**
     * check the voter has the permission to use the action
     *
     * @param voter the player who initiate the action
     * @return ture the player can perform this action <br/>
     * false the player can't perform this action
     */
    private boolean isActionAuthorised(final StompPrincipal voter, final GameService.RoleAction action) {

        if (voter.getRole().equals(Role.WOLF)) {
            return KILL.equals(action);
        } else if (voter.getRole().equals(Role.WITCH)) {
            return POISONING.equals(action) || SAVE.equals(action) || NOTHING.equals(action);
        } else if (voter.getRole().equals(Role.SEER)) {
            return CHECK.equals(action);
        } else {
            log.info("no matching role found:{}", voter.getRole().name());
            return false;
        }
    }

}
