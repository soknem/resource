package com.setec.resource.feature.file;


import com.setec.resource.domain.CompressLevel;
import com.setec.resource.domain.FileType;
import com.setec.resource.feature.file.dto.*;
import io.minio.errors.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * File interface which contains methods to manage file upload,view,delete
 *
 * @author Pov soknem
 * @since 1.0 (2024)
 */
public interface FileService {

    FileResponse uploadSingleFile(MultipartFile file, boolean compress, CompressLevel level, FileType type);

    /**
     * Uploads a single media file.
     *
     * @param file is the file to upload
     * @return {@link FileResponse}
     * @author Pov soknem
     * @since 1.0 (2024)
     */
    FileResponse uploadSingleFile(MultipartFile file);

    /**
     * get all file metadata and url
     *
     * @return {@link List<FileResponse>}
     * @author Pov soknem
     * @since 1.0 (2024)
     */
    List<FileResponse> loadAllFiles();

    void delete(List<FileDeleteRequest> fileDeleteRequests);


    List<FileNameResponse> getAllFileNames();

    /**
     * get file by filename
     *
     * @param fileName is the file name to get
     * @return {@link FileResponse}
     * @author Pov soknem
     * @since 1.0 (2024)
     */


    FileResponse loadFileByName(String fileName);

    /**
     * delete file by file name
     *
     * @param fileName is the filename to delete
     * @author Pov soknem
     * @since 1.0 (2024)
     */

    void deleteFileByName(String fileName);

    /**
     * download file by file name
     *
     * @param fileName is the file name to download
     * @return {@link  Resource}
     * @author Pov soknem
     * @since 1.0 (2024)
     */
    Resource downloadFileByName(String fileName);

//    /**
//     * view file by file name
//     *
//     * @param fileName the name of the file to check for existence
//     * @return {@link FileViewResponse}
//     * @throws InsufficientDataException       if not enough data is available
//     * @throws ErrorResponseException          if an error response is received from the server
//     * @throws IOException                     if an I/O error occurs
//     * @throws NoSuchAlgorithmException        if the specified algorithm is not available
//     * @throws InvalidKeyException             if the key is invalid
//     * @throws InvalidResponseException        if the response from the server is invalid
//     * @throws XmlParserException              if an error occurs while parsing XML
//     * @throws InternalException               if an internal error occurs
//     * @throws ServerException if a server-side error occurs
//     * @author Pov soknem
//     * @since 1.0 (2024)
//     */
//    FileViewResponse viewFileByFileName(String fileName) throws InsufficientDataException,
//            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ServerException;


    Resource viewFileRange(String fileName, String rangeHeader);

    /**
     * Handles both full and partial (Range) file streaming logic.
     * @param fileName the name of the file
     * @param rangeHeader the HTTP Range header from the request
     * @return {@link FileStreamResponse}
     */
    FileStreamResponse getFileStream(String fileName, String rangeHeader);


    FileViewResponse getBackground(String type);
}