package org.example.controller;

import org.example.model.ActionResult;
import org.example.model.Role;
import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.example.service.GameStepService;
import org.example.service.PlayerService;
import org.example.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Created by daqwang on 14/12/20.
 */
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

    @GetMapping("/action/{action}/voter/{voterNickname}/player/{nickName}/")
    public ResponseEntity vote(@PathVariable("action") String action, @PathVariable("voterNickname") String voterNickname, @PathVariable("nickName") String nickName) {
        StompPrincipal voter = playerService.getPlayerByName(voterNickname);
        GameService.ACTION playerAction = GameService.ACTION.valueOf(action.trim().toUpperCase());
        switch (playerAction) {
            case CHECK:
                return handleSuspectAction(nickName);
            case KILL:
                return handleWolfKillAction(voter, nickName);
            case SAVE:
                return handleWitchHelpAction(voter, nickName);
            case POISON:
                return handleWitchPoisonAction(nickName);
            default:
                throw new RuntimeException("can't find match action" + action);
        }
    }

    @GetMapping("/test/votedRole/{role}")
    public ResponseEntity<String> testVotedRole(@PathVariable("role") String role) {
        List<StompPrincipal> inGamePlayersByRole =
                playerService.getInGamePlayersByRole(Role.valueOf(role));
        inGamePlayersByRole.stream().forEach(player -> {
            player.setHasVoted(true);
        });

        return ResponseEntity.ok("set " + inGamePlayersByRole.size() + " voted for " + role);
    }

    /**
     * handle vote result, check everyone is voted and proceed the next game step.
     *
     * @param nickName
     * @return
     */
    private ResponseEntity handleSuspectAction(String nickName) {

        return null;
    }


    private ResponseEntity handleWitchPoisonAction(String nickName) {
        return null;
    }

    /**
     * handle wolf kill action by player nick name.
     *
     * @param voter    the wolf player
     * @param nickName the player will be killed
     * @return
     */
    private ResponseEntity handleWolfKillAction(final StompPrincipal voter, final String nickName) {


        return null;
    }


    /**
     * witch get player identity by player nick name.
     *
     * @param nickName player name want to be identified by witch
     * @return GameMessage
     */
    private ResponseEntity<ActionResult> handleWitchHelpAction(final StompPrincipal voter, final String nickName) {
        boolean authorised = hasIdentityAuthorised(voter, Role.WITCH);
        if (!authorised) {
            return ResponseEntity.ok(new ActionResult("player:" + voter.getNickName() + " is " + voter.getRole().name() + ", can't perform check action"));
        }
        Role role = gameService.checkPlayerRole(nickName);

        if (role == null) {

            String message = "Sorry can't find player " + nickName + " maybe player is not in game.";
            // TODO 24/12/20
            // need to use different object for result need to consider voteReport

            return ResponseEntity.ok(new ActionResult(message));
        }
        String message = "Player " + nickName + " role is " + role.name();
        ActionResult actionResult = new ActionResult(message);
        return ResponseEntity.ok(actionResult);
    }

    /**
     * check the voter has the permission to use the action
     *
     * @param voter the player who initiate the action
     * @return ture the player can perform this action <br/>
     * false the player can't perform this action
     */
    private boolean hasIdentityAuthorised(final StompPrincipal voter, final Role voterRole) {
        return voter.getRole().equals(voterRole);
    }

}
