package org.example.websockets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.example.utils.EndpointConstant.BROADCAST_MESSAGE_ENDPOINT;
import static org.example.utils.EndpointConstant.PRIVATE_MESSAGE_ENDPOINT;

@Slf4j
@Controller
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final static String DESTINATION = "/broadcast/messages";
    private final static String PRIVATE_DESTINATION = "/private/messages";

    @Autowired
    public ChatController(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }


    @MessageMapping(BROADCAST_MESSAGE_ENDPOINT)
    @SendTo(DESTINATION)
    public OutputMessage sendToBroadcast(final Message message, @Header("simpSessionId") String sessionId, Principal principal) {
        log.info("Received broadcast greeting message {} from {} with sessionId {}", message.getText(), principal.getName(), sessionId);

        final String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        OutputMessage outputMessage = new OutputMessage(message.getFrom(), message.getText(), time, principal.getName());
        simpMessagingTemplate.convertAndSend(DESTINATION, outputMessage);
        return outputMessage;
    }


    @MessageMapping(PRIVATE_MESSAGE_ENDPOINT)
    @SendToUser(PRIVATE_DESTINATION)
    public OutputMessage send(final Message message, @Header("simpSessionId") String sessionId, Principal principal) {
        log.info("Received private greeting message {} from {} with sessionId {}", message.getText(), principal.getName(), sessionId);

        final String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        OutputMessage outputMessage = new OutputMessage(message.getFrom(), message.getText(), time, principal.getName());
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), PRIVATE_DESTINATION, outputMessage);
        return outputMessage;
    }


//    @GetMapping("/testPrivateMsg/{username}")
//    @SendToUser(DESTINATION)
//    public String testPrivateMsg(@PathVariable("username") String username) {
//        final String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
//        OutputMessage outputMessage = new OutputMessage("System", "private message", time);
//        Principal principal = players.get(username);
//        if (principal != null) {
//            log.info("receiver:{},destination:{}", principal.getName(), DESTINATION);
//            simpMessagingTemplate.convertAndSendToUser(principal.getName(), DESTINATION, outputMessage);
//        } else {
//            log.info("can't find receiver:{}", username);
//        }
//
//        return "test";
//    }

}
