package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.model.ActionResult;
import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.example.service.GameStepService;
import org.example.service.PlayerService;
import org.example.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
     * @param action    role action (kill,check,save,poisoning...)
     * @param voterName the name of the player who make the vote
     * @param name      the player's name who is been voted
     * @return ActionResult
     */
    @GetMapping("/voter/{voterName}/player/{name}/action/{action}")
    public ResponseEntity<ActionResult> action(@PathVariable("action") String action, @PathVariable("voterName") String voterName, @PathVariable("name") String name) {
        //todo update to playerName

        StompPrincipal voter = playerService.getPlayerByName(voterName);
        GameService.RoleAction playerAction = GameService.RoleAction.valueOf(action.trim().toUpperCase());
        //check if player has the right permission to perform the action
        if (!isActionAuthorised(voter, playerAction)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ActionResult("player can't perform this action:" + action));
        }
        ActionResult actionResult = voteService.handleRoleAction(voter, name, playerAction);
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

        switch (voter.getRole()) {
            case WOLF:
                return KILL.equals(action);
            case WITCH:
                return POISONING.equals(action) || SAVE.equals(action) || NOTHING.equals(action);
            case SEER:
                return CHECK.equals(action);
            default:
                log.info("no matching role found:{}", voter.getRole().name());
                return false;
        }
    }

    /**
     * everyone vote for the person who has the suspec
     *
     * @param voterNickname
     * @param playerNickname
     * @return result of vote
     */
    @GetMapping(value = "/suspect/voterNickname/{voterNickname}/player/{playerNickname}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> vote(@PathVariable("voterNickname") String voterNickname, @PathVariable("playerNickname") String playerNickname) {
        StompPrincipal voter = playerService.getPlayerByNickName(voterNickname);
        if (validateVote(voterNickname, playerNickname)) {
            voteService.vote(voter, playerNickname);
            voteService.broadcastVoteStatus();
            return ResponseEntity.ok("vote success");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("voter is not valida, voter has voted:" + voter.isHasVoted());
        }
    }

    /**
     * check the voter is inGame haven't vote and the player nickname is different from voter nick name.
     *
     * @param voterNickname  voter's nick name
     * @param playerNickname other players the voter want to vote
     * @return true: the nick names are valida and not empty, the voter is in game and haven't vote<br/>
     * false: the voter is not in game or the nick names maybe empty
     */
    private boolean validateVote(final String voterNickname, final String playerNickname) {
        if (voterNickname.equalsIgnoreCase(playerNickname) || StringUtils.isBlank(voterNickname)) {
            return false;
        }
        StompPrincipal voter = playerService.getPlayerByNickName(voterNickname);
        return voter.isInGame() && !voter.isHasVoted();
    }
}
