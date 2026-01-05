package com.setec.resource.feature.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.setec.resource.domain.FileType;
import lombok.Builder;

@Builder
public record FileResponse(
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String contentType,
        String extension,
        String uri,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long size,
        FileType type
) {

}