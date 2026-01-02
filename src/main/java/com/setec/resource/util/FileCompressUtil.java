package com.setec.resource.util;

import com.setec.resource.domain.CompressLevel;

public class FileCompressUtil {

    public static double getCompressValue(CompressLevel compressLevel) {
        if (compressLevel == CompressLevel.LOW) {
            return 0.8;
        } else if (compressLevel == CompressLevel.MEDIUM) {
            return 0.6;
        } else if (compressLevel == CompressLevel.HIGH) {
            return 0.4;
        } else if (compressLevel == CompressLevel.EXTREME) {
            return 0.2;
        }else if (compressLevel == CompressLevel.ULTRA) {
            return 0.1;
        }
        else {
            return 1;
        }
    }
}
