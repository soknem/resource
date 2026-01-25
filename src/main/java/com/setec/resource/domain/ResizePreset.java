package com.setec.resource.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@AllArgsConstructor
public enum ResizePreset {
    ORIGINAL(0, 0),
    CARD(400, 400),
    BACKGROUND(1920, 1080),
    THUMBNAIL(150, 150);

    private final int width;
    private final int height;

}