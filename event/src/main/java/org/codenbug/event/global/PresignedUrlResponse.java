package org.codenbug.event.global;

import lombok.Getter;

@Getter
public class PresignedUrlResponse {
    private final String fileName;
    private final String url;

    public PresignedUrlResponse(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }
}