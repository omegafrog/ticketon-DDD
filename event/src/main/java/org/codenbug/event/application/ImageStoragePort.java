package org.codenbug.event.application;

import java.io.IOException;
import java.util.List;

import org.codenbug.event.global.PresignedUrlResponse;

public interface ImageStoragePort {

	List<PresignedUrlResponse> generateUploadUrls(List<String> fileNames);

	String store(byte[] fileData, String fileName) throws IOException;

	void delete(String fileName) throws IOException;

	String publicPath(String fileName);
}
