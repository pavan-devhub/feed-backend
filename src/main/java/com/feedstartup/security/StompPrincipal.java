package com.feedstartup.security;

import java.security.Principal;

/**
 * The logged-in user a STOMP connection belongs to, set on CONNECT by StompAuthInterceptor. Its
 * name is what SimpMessagingTemplate#convertAndSendToUser addresses, so a message for "user-42"
 * reaches every tab and device user 42 has open.
 */
public record StompPrincipal(String name) implements Principal {

    public static StompPrincipal forUser(Long userId) {
        return new StompPrincipal("user-" + userId);
    }

    @Override
    public String getName() {
        return name;
    }
}
