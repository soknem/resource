package com.setec.resource.feature.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
//@RefreshScope
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public void uploadFile(InputStream inputStream, long size, String contentType, String objectName) throws Exception {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new Exception("File upload failed: " + e.getMessage(), e);
        } finally {
            inputStream.close(); // Ensure stream is closed
        }
    }

    @Override
    public void uploadFile(MultipartFile file, String objectName) throws Exception {
        uploadFile(file.getInputStream(), file.getSize(), file.getContentType(), objectName);
    }

    @Override
    public InputStream getFile(String objectName) throws Exception {

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (MinioException e) {
            throw new Exception("Error occurred: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String objectName) throws Exception {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (MinioException e) {
            throw new Exception("Error occurred: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFile(String objectName, long offset, long length) throws Exception {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .offset(offset)
                            .length(length)
                            .build()
            );
        } catch (MinioException e) {
            throw new Exception("Error occurred while fetching range: " + e.getMessage(), e);
        }
    }
}