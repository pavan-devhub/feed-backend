package com.feedstartup.exception;

import com.feedstartup.dto.ActiveSessionDto;

import java.util.List;

public class TooManySessionsException extends RuntimeException {

    private final List<ActiveSessionDto> activeSessions;

    public TooManySessionsException(String message, List<ActiveSessionDto> activeSessions) {
        super(message);
        this.activeSessions = activeSessions;
    }

    public List<ActiveSessionDto> getActiveSessions() {
        return activeSessions;
    }
}
