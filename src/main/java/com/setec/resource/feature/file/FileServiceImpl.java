package com.setec.resource.feature.file;


import com.setec.resource.domain.File;
import com.setec.resource.feature.file.dto.FileResponse;
import com.setec.resource.feature.file.dto.FileViewResponse;
import com.setec.resource.feature.minio.MinioService;
import com.setec.resource.util.MediaUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
//@RefreshScope
public class FileServiceImpl implements FileService {

    private final MinioService minioService;

    private final FileRepository fileRepository;

    private final MinioClient minioClient;

    @Value("${media.base-uri}")
    private String baseUri;

    //endpoint that handle manage medias
    @Value("${media.image-end-point}")
    private String imageEndpoint;

    @Value("${minio.bucket-name}")
    String bucketName;

    @Override
    public FileResponse uploadSingleFile(MultipartFile file) {

        String folderName = getValidFolder(file);

        String extension = MediaUtil.extractExtension(Objects.requireNonNull(file.getOriginalFilename()));

        String newName;
        do {
            newName = UUID.randomUUID().toString();
        } while (fileRepository.existsByFileName(newName + "." + extension));


        String objectName = folderName + "/" + newName + "." + extension;

        try {
            minioService.uploadFile(file, objectName);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        //create new object that store file metadata
        File fileObject = new File();

        //set all field
        fileObject.setFileName(newName + "." + extension);

        fileObject.setFileSize(file.getSize());

        fileObject.setContentType(file.getContentType());

        fileObject.setFolder(folderName);

        fileObject.setExtension(extension);

        //save file metadata to database
        fileRepository.save(fileObject);

        //response to DTO
        return FileResponse.builder()
                .name(newName + "." + extension)
                .contentType(file.getContentType())
                .extension(extension)
                .size(file.getSize())
                .uri(baseUri + imageEndpoint + "/view/" + newName + "." + extension)
                .build();
    }

    @Override
    public List<FileResponse> loadAllFiles() {

        // Fetch all images from the repository
        List<File> files = fileRepository.findAll();

        // Map each File entity to an FileResponse DTO
        List<FileResponse> responses = new ArrayList<>();
        for (File file : files) {
            FileResponse response = FileResponse.builder()
                    .name(file.getFileName())
                    .contentType(file.getContentType())
                    .extension(file.getExtension())
                    .size(file.getFileSize())
                    .uri(baseUri + imageEndpoint + "/view/" + file.getFileName())
                    .build();
            responses.add(response);
        }

        return responses;
    }

    @Override
    public FileResponse loadFileByName(String fileName) {

        try {
            String contentType = getContentType(fileName);

            String extension = MediaUtil.extractExtension(fileName);

            return FileResponse.builder()
                    .name(fileName)
                    .contentType(contentType)
                    .extension(extension)
                    .uri(baseUri + imageEndpoint + "/view/" + fileName)
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    @Override
    public void deleteFileByName(String fileName) {

        try {
            String contentType = getContentType(fileName);

            String folderName = contentType.split("/")[0];

            String objectName = folderName + "/" + fileName;

            minioService.deleteFile(objectName);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    @Override
    public Resource downloadFileByName(String mediaName) {

        try {
            String contentType = getContentType(mediaName);

            String folderName = contentType.split("/")[0];

            String objectName = folderName + "/" + mediaName;

            InputStream inputStream = minioService.getFile(objectName);

            Path tempFile = Files.createTempFile("minio", mediaName);

            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            return new UrlResource(tempFile.toUri());

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media has not been found!");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public FileViewResponse viewFileByFileName(String fileName) {

        // Fetch file metadata from the repository
        File image = fileRepository.findByFileName(fileName).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "File has not been found!"));

        // Construct the object path in MinIO
        Path path = Path.of(image.getFileName());
        String objectPath = image.getFolder() + "/" + path;


        // Fetch the object from MinIO
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectPath)
                .build();

        InputStream inputStream;
        try {
            inputStream = minioClient.getObject(getObjectArgs);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching file from storage", e);
        }

        // Wrap the InputStream in an InputStreamResource
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        // Construct and return the response
        return FileViewResponse.builder()
                .fileName(image.getFileName())
                .fileSize(image.getFileSize())
                .contentType(image.getContentType())
                .stream(inputStreamResource)
                .build();
    }


    private String getContentType(String fileName) {
        File fileObject = fileRepository.findByFileName(fileName).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("file = %s has not been found", fileName)));
        return fileObject.getContentType();
    }

    private static String getValidFolder(MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType == null || !((contentType.startsWith("video/") || contentType.startsWith("image/") || contentType.equals("application/pdf")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type.");
        }

        return contentType.split("/")[0];
    }
}