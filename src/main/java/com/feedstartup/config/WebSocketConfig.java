package com.feedstartup.config;

import com.feedstartup.security.StompAuthInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket at /ws, for live updates: the navbar bell, Status of Activities and the EPM
 * lists listen here and reload when told something changed (see LiveUpdate, LiveUpdateBroadcaster).
 * The handshake itself is open (SecurityConfig's permitAll) - the login happens in the STOMP
 * CONNECT frame, see StompAuthInterceptor.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;
    private final TaskScheduler heartbeatScheduler;

    // @Lazy: Spring creates this scheduler from this very configuration, so it can only be looked up once used.
    public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor,
                           @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler heartbeatScheduler) {
        this.stompAuthInterceptor = stompAuthInterceptor;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Any origin, like the REST API's CORS setting in SecurityConfig.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // The in-memory broker suits one backend instance. Several behind a load balancer would
        // need a shared broker (RabbitMQ / ActiveMQ through enableStompBrokerRelay) so a message
        // reaches browsers connected to any of them.
        // Heartbeats every 10 seconds both ways let each side notice a dead connection (a sleeping
        // laptop, dropped Wi-Fi, a proxy's idle timeout) so the browser reconnects.
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {10_000, 10_000})
                .setTaskScheduler(heartbeatScheduler);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
