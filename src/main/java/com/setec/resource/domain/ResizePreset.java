package com.setec.resource.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ResizePreset {
    CUSTOM(-1,-1),
    ORIGINAL(0, 0),
    ICON(250, 250),
    AVATAR(400, 400),
    SD(720, 480),
    HD(1280, 720),
    FHD(1920, 1080),
    QHD(2560, 1440),
    UHD(4096, 2160),
    UHD_2(8192, 4320);

    private final int width;
    private final int height;

}