package com.feedstartup.exception;

/** The caller is logged in, but this action isn't theirs to take (e.g. only the default admin removes admins). Mapped to 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
