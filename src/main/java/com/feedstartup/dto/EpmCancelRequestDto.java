package com.feedstartup.dto;

import jakarta.validation.constraints.Size;

/** Admin payload for calling off an EPM. The reason is optional and shown to registered people. */
public record EpmCancelRequestDto(@Size(max = 500, message = "Reason must be at most 500 characters") String reason) {}
