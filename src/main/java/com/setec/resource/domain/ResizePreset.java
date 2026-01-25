package com.setec.resource.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ResizePreset {
    CUSTOM(-1,-1),
    ORIGINAL(0, 0),
    CARD(400, 400),
    BACKGROUND(1920, 1080),
    THUMBNAIL(150, 150);


    private final int width;
    private final int height;

}