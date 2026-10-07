package com.feedstartup.exception;

import java.util.List;

/**
 * A new account's username, mobile number or email is already in use. Says which of its own
 * fields clash - never by whom (an admin, a user, which one). Mapped to 409 with a
 * {@code fields} map the form shows next to each field (see GlobalExceptionHandler).
 */
public class AlreadyTakenException extends ConflictException {

    private final List<String> fields;

    /** @param fields the request's field names that clash, e.g. "username", "mobileNumber", "email" */
    public AlreadyTakenException(List<String> fields, String message) {
        super(message);
        this.fields = List.copyOf(fields);
    }

    public List<String> getFields() {
        return fields;
    }
}
