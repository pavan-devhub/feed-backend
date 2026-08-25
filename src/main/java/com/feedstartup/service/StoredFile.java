package com.feedstartup.service;

import org.springframework.core.io.Resource;

/** A file streamed back to the client, together with the headers the controller needs to set. */
public record StoredFile(Resource resource, String contentType, String filename) {}
