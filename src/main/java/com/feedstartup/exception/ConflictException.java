package com.feedstartup.exception;

/** The request is valid but clashes with existing data (a duplicate name, a category still in use). Mapped to 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
