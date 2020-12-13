package org.example.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.model.GameMessage;
import org.example.model.StompPrincipal;
import org.example.service.GameService;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

import static org.example.utils.EndpointConstant.BROADCAST_DESTINATION;

/**
 * Created by daqwang on 12/12/20.
 */
@Component
@Slf4j
public class Scheduler {

    private final GameService gameService;
    private final PlayerService playerService;
    private final SimpMessagingTemplate simpMessagingTemplate;


    @Value("${total.player.number}")
    private int totalPlayers;

    @Autowired
    public Scheduler(final GameService gameService, final PlayerService playerService, final SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.playerService = playerService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 1000)
    public void startGame() throws InterruptedException {
        log.info("scheduled task started");

        if (!isGameStarted()) {
            simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage("Assign roles in 5 seconds..."));
            Thread.sleep(5000);
            List<StompPrincipal> readyPlayerList = playerService.getReadyPlayerList();
            if (!CollectionUtils.isEmpty(readyPlayerList) && totalPlayers == readyPlayerList.size()) {
                gameService.deal();
                simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage("Roles have been assigned!"));
            }
        }
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

//    @Scheduled(fixedDelay = 15000, initialDelay = 1000)
//    public void testBroadcastMessage() throws InterruptedException {
//        log.info("testBroadcastMessage started");
//
//
//        LocalDateTime localDateTime = LocalDateTime.now().plusSeconds(5);
//        Date timeToStart = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
//        final String time = new SimpleDateFormat("HH:mm:ss").format(timeToStart);
//        simpMessagingTemplate.convertAndSend(BROADCAST_DESTINATION, new GameMessage(RandomStringUtils.random(15)));
//
//
//    }

    public boolean isGameStarted() {
        List<StompPrincipal> readyPlayerList = playerService.getReadyPlayerList();
        if (!CollectionUtils.isEmpty(readyPlayerList) && totalPlayers == readyPlayerList.size()) {
            //check each ready player has roles
            Optional<StompPrincipal> anyPlayerNoRoleAssign = readyPlayerList.stream().filter(readyPlayer -> readyPlayer.getRole() == null).findAny();
            //return false if any player not assigned with a role
            return !anyPlayerNoRoleAssign.isPresent();
        }
        return true;
    }

}
