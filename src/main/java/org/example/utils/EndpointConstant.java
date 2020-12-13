package org.example.utils;

/**
 * Created by daqwang on 29/11/20.
 */
public class EndpointConstant {
    //endpoints for server to receive client messages
    public final static String PRIVATE_MESSAGE_ENDPOINT = "/chat/private";
    public final static String BROADCAST_MESSAGE_ENDPOINT = "/chat/broadcast";

    //destinations for server to send messages to client
    public final static String BROADCAST_DESTINATION = "/broadcast/messages";
    public final static String BROADCAST_PLAYER_STATUS_DESTINATION = "/broadcast/player/status";
    public final static String PRIVATE_DESTINATION = "/private/messages";
    public final static String PRIVATE_ROLE_DESTINATION = "/private/role";
}
