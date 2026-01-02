package com.setec.resource.feature.file;


import com.setec.resource.domain.File;
import com.setec.resource.feature.file.dto.FileResponse;
import com.setec.resource.feature.file.dto.FileStreamResponse;
import com.setec.resource.feature.file.dto.FileViewResponse;
import com.setec.resource.feature.minio.MinioService;
import com.setec.resource.util.MediaUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

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
    public FileResponse uploadSingleFile(MultipartFile file, boolean compress) {

        String contentType = file.getContentType();
        String folderName = getValidFolder(file);

        if (!contentType.startsWith("image/")) {
            compress = false; // Only compress images for now
        }

        String originalExtension = MediaUtil.extractExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = compress ? "jpg" : originalExtension; // Convert to JPG for better compression
        contentType = compress ? "image/jpeg" : contentType;

        String newName;
        do {
            newName = UUID.randomUUID().toString();
        } while (fileRepository.existsByFileName(newName + "." + extension));

        String objectName = folderName + "/" + newName + "." + extension;

        long size = file.getSize();
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            if (compress) {
                long[] compressedSize = new long[1];
                InputStream compressedStream = compressImage(inputStream, compressedSize);
                size = compressedSize[0];
                inputStream.close();
                inputStream = compressedStream;
            }
            minioService.uploadFile(inputStream, size, contentType, objectName);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    // Ignore close errors
                }
            }
        }

        //create new object that store file metadata
        File fileObject = new File();

        //set all field
        fileObject.setFileName(newName + "." + extension);

        fileObject.setFileSize(size);

        fileObject.setContentType(contentType);

        fileObject.setFolder(folderName);

        fileObject.setExtension(extension);

        //save file metadata to database
        fileRepository.save(fileObject);

        //response to DTO
        return FileResponse.builder()
                .name(newName + "." + extension)
                .contentType(contentType)
                .extension(extension)
                .size(size)
                .uri(baseUri + imageEndpoint + "/view/" + newName + "." + extension)
                .build();
}

    private InputStream compressImage(InputStream inputStream, long[] outSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(inputStream)
                .scale(1.0) // No resize, just compress
                .outputQuality(0.8) // 80% quality; adjust 0.0-1.0 (lower = more compression)
                .outputFormat("jpg") // Force JPG for best size reduction
                .toOutputStream(baos);

        outSize[0] = baos.size();
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public FileResponse uploadSingleFile(MultipartFile file) {
        return null;
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

        File file = fileRepository.findByFileName(fileName).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,String.format("file = %s has not been found",fileName)));

        fileRepository.delete(file);

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

    @Override
    public Resource viewFileRange(String fileName, String rangeHeader) {
        File fileMetadata = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        long fileSize = fileMetadata.getFileSize();
        String objectPath = fileMetadata.getFolder() + "/" + fileMetadata.getFileName();

        // Default values for the whole file
        long start = 0;
        long end = fileSize - 1;

        // Parse Range Header: "bytes=start-end"
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid range format");
            }
        }

        if (start > end || start >= fileSize) {
            throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        long contentLength = (end - start) + 1;

        try {
            // Fetch only the requested range from MinIO
//            InputStream inputStream = minioClient.getObject(
//                    GetObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(objectPath)
//                            .offset(start)
//                            .length(contentLength)
//                            .build()
//            );
//            return new InputStreamResource(inputStream);
            InputStream inputStream = minioService.getFile(objectPath, start, contentLength);
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO error", e);
        }
    }

    @Override
    public FileStreamResponse getFileStream(String fileName, String rangeHeader) {
        File fileMetadata = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        long fileSize = fileMetadata.getFileSize();
        String objectPath = fileMetadata.getFolder() + "/" + fileMetadata.getFileName();

        long start = 0;
        long end = fileSize - 1;
        boolean isPartial = false;

        // Parse Range Header
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            isPartial = true;
            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Math.min(Long.parseLong(ranges[1]), fileSize - 1);
                }
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Range header");
            }
        }

        if (start > end || start >= fileSize) {
            throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        try {
            long contentLength = (end - start) + 1;
            // Call your MinIO service range method
            InputStream inputStream = minioService.getFile(objectPath, start, contentLength);
            Resource resource = new InputStreamResource(inputStream);

            return new FileStreamResponse(
                    resource,
                    fileMetadata.getContentType(),
                    fileSize,
                    start,
                    end,
                    isPartial
            );
        } catch (Exception e) {
            log.error("Error streaming file: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Streaming failed");
        }
    }
}