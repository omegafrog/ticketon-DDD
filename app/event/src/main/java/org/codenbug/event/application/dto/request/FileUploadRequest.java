package org.codenbug.event.application.dto.request;

import java.util.List;
import lombok.Getter;

@Getter
public class FileUploadRequest {

    private List<String> fileNames;

    protected FileUploadRequest() {
    }

    public FileUploadRequest(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}