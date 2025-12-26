package com.setec.resource.feature.file.dto;

import org.springframework.core.io.Resource;

public record FileStreamResponse(
    Resource resource,
    String contentType,
    long fileSize,
    long start,
    long end,
    boolean isPartial
) {}