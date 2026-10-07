package com.feedstartup.security;

import com.feedstartup.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Who a STOMP connection is logged in as, and which destinations it may subscribe to. */
class StompAuthInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final StompAuthInterceptor interceptor = new StompAuthInterceptor(jwtUtil, sessionService);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void aUserTokenWithAnActiveSessionConnectsAsThatUser() {
        token("user-token", "jti-1", true, 42L);

        assertEquals("user-42", connect("Bearer user-token").getName());
    }

    @Test
    void aLoggedOutTokenConnectsAsAGuest() {
        token("old-token", "jti-2", false, 42L);

        assertNull(connect("Bearer old-token"));
    }

    @Test
    void anAdminTokenConnectsAsAGuest() {
        // An admin's session lives in system_admin_sessions, so user_sessions doesn't know it.
        token("admin-token", "jti-3", false, null);

        assertNull(connect("Bearer admin-token"));
    }

    @Test
    void noTokenOrAGarbledOneConnectsAsAGuest() {
        when(jwtUtil.validateToken("garbled")).thenThrow(new RuntimeException("bad token"));

        assertNull(connect(null));
        assertNull(connect("Bearer garbled"));
    }

    @Test
    void anyoneMaySubscribeToThePublicTopics() {
        assertNotNull(subscribe("/topic/epm-events", null));
        assertNotNull(subscribe("/topic/publications", null));
    }

    @Test
    void onlyALoggedInUserMaySubscribeToTheirOwnQueue() {
        assertNotNull(subscribe("/user/queue/updates", StompPrincipal.forUser(7L)));
        assertNull(subscribe("/user/queue/updates", null));
    }

    @Test
    void anythingElseIsDroppedQuietly() {
        // Another user's resolved queue, an unknown topic, and messages from the browser.
        assertNull(subscribe("/queue/updates-user1a2b3c", StompPrincipal.forUser(7L)));
        assertNull(subscribe("/topic/admin", StompPrincipal.forUser(7L)));
        assertNull(interceptor.preSend(frame(StompCommand.SEND, a -> a.setDestination("/topic/epm-events")), channel));
    }

    private void token(String jwt, String jti, boolean activeUserSession, Long userId) {
        when(jwtUtil.validateToken(jwt)).thenReturn(true);
        when(jwtUtil.extractJti(jwt)).thenReturn(jti);
        when(sessionService.isActive(jti)).thenReturn(activeUserSession);
        when(jwtUtil.extractUserId(jwt)).thenReturn(userId);
    }

    private Principal connect(String authorization) {
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, a -> {
            if (authorization != null) a.setNativeHeader("Authorization", authorization);
        }), channel);
        assertNotNull(result, "CONNECT is never refused");
        return StompHeaderAccessor.wrap(result).getUser();
    }

    private Message<?> subscribe(String destination, Principal user) {
        return interceptor.preSend(frame(StompCommand.SUBSCRIBE, a -> {
            a.setDestination(destination);
            a.setUser(user);
        }), channel);
    }

    private static Message<byte[]> frame(StompCommand command, Consumer<StompHeaderAccessor> setUp) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        setUp.accept(accessor);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
