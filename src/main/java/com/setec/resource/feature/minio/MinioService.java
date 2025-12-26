package com.setec.resource.feature.minio;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Minio interface which contains methods for connect to minio server
 *
 * @author Pov soknem
 * @since 1.0 (2024)
 */
public interface MinioService {

    /**
     * upload file
     *
     * @param file       is the file to upload
     * @param objectName is the folder name and filename
     * @throws Exception catch exception when fail to upload
     * @author Pov soknem
     * @since 1.0 (2024)
     */
    void uploadFile(MultipartFile file, String objectName) throws Exception;

    /**
     * get file
     *
     * @param objectName is the folder name and filename
     * @return {@link  InputStream}
     * @throws Exception catch exception when fail to upload
     * @author Pov soknem
     * @since 1.0 (2024)
     */

    InputStream getFile(String objectName) throws Exception;

    /**
     * delete file
     *
     * @param objectName is the folder name and filename
     * @throws Exception catch exception when fail to upload
     * @author Pov soknem
     * @since 1.0 (2024)
     */

    void deleteFile(String objectName) throws Exception;
}