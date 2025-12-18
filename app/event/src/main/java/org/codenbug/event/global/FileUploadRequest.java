package org.codenbug.event.global;

import java.util.List;

import lombok.Getter;

@Getter
public class FileUploadRequest {
    private List<String> fileNames;

    protected FileUploadRequest() {}

    public FileUploadRequest(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}