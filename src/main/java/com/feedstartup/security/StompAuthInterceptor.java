package com.feedstartup.security;

import com.feedstartup.realtime.LiveUpdate;
import com.feedstartup.service.SessionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;

/**
 * Logs a STOMP connection in, and keeps each one to what it may hear.
 * <p>
 * A browser can't put an Authorization header on a WebSocket, so it sends the JWT in the STOMP
 * CONNECT frame's own "Authorization" header instead, checked the way JwtAuthenticationFilter
 * checks a REST request: a valid token whose user_sessions row is still active. Anything else - no
 * token, a logged-out one, or an admin's - still connects, as a guest that only hears the public
 * topics. Messages only ever say what changed (see LiveUpdate), so the public topics tell a guest
 * nothing the public REST API wouldn't.
 * <p>
 * A subscription that isn't allowed is dropped quietly rather than answered with an ERROR frame,
 * which would close the whole connection and send the browser into a reconnect loop.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Set<String> PUBLIC_TOPICS = Set.of(LiveUpdate.EPM_EVENTS_TOPIC, LiveUpdate.PUBLICATIONS_TOPIC);
    private static final String MY_UPDATES = "/user" + LiveUpdate.MY_UPDATES_QUEUE;

    private final JwtUtil jwtUtil;
    private final SessionService sessionService;

    public StompAuthInterceptor(JwtUtil jwtUtil, SessionService sessionService) {
        this.jwtUtil = jwtUtil;
        this.sessionService = sessionService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> {
                // Spring remembers this user for the rest of the connection.
                StompPrincipal user = authenticate(accessor.getFirstNativeHeader("Authorization"));
                if (user != null) accessor.setUser(user);
            }
            case SUBSCRIBE -> {
                if (!maySubscribe(accessor.getDestination(), accessor.getUser())) return null;
            }
            // The server only talks; nothing listens for messages from the browser.
            case SEND -> {
                return null;
            }
            default -> { }
        }
        return message;
    }

    private static boolean maySubscribe(String destination, Principal user) {
        if (destination == null) return false;
        if (PUBLIC_TOPICS.contains(destination)) return true;
        return MY_UPDATES.equals(destination) && user instanceof StompPrincipal;
    }

    private StompPrincipal authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) return null;
        String jwt = header.substring(7);
        try {
            if (!jwtUtil.validateToken(jwt) || !sessionService.isActive(jwtUtil.extractJti(jwt))) return null;
            Long userId = jwtUtil.extractUserId(jwt);
            return userId == null ? null : StompPrincipal.forUser(userId);
        } catch (RuntimeException e) {
            return null; // a garbled token is just a guest
        }
    }
}
