package org.codenbug.event.global;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class FileUploadRequest {
	@NotNull
	@NotEmpty
    private List<String> fileNames;

    protected FileUploadRequest() {}

    public FileUploadRequest(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}
