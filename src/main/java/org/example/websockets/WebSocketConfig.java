package org.example.websockets;

import org.example.handler.PlayerHandshakeHandler;
import org.example.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static org.example.utils.EndpointConstant.BROADCAST_MESSAGE_ENDPOINT;
import static org.example.utils.EndpointConstant.PRIVATE_MESSAGE_ENDPOINT;

@Configuration
@EnableWebSocketMessageBroker
@ComponentScan("org.example")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    private static final String CONNECT = "/connect";
    private final PlayerService playerService;

    @Autowired
    public WebSocketConfig(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry config) {
        config.enableSimpleBroker("/broadcast", "/private");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {

        registry.addEndpoint(CONNECT, PRIVATE_MESSAGE_ENDPOINT, BROADCAST_MESSAGE_ENDPOINT)
                .setAllowedOrigins("*")
                .setHandshakeHandler(getPlayerHandshakeHandler())
                .withSockJS();

        registry.addEndpoint(CONNECT, PRIVATE_MESSAGE_ENDPOINT, BROADCAST_MESSAGE_ENDPOINT)
                .setAllowedOrigins("*")
                .setHandshakeHandler(getPlayerHandshakeHandler());
    }

    /**
     * 配置客户端入站通道拦截器
     */
//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        registration.setInterceptors(createUserInterceptor());
//
//    }
    @Bean
    public PlayerHandshakeHandler getPlayerHandshakeHandler() {
        return new PlayerHandshakeHandler(this.playerService);
    }


//    @Bean
//    public HandshakeInterceptor createSessionInterceptor(){
//        return new SessionAuthHandshakeInterceptor();
//    }
//    /*将客户端渠道拦截器加入spring ioc容器*/
//    @Bean
//    public UserInterceptor createUserInterceptor() {
//        return new UserInterceptor();
//    }

}