package com.setec.resource.feature.file;

import com.setec.resource.domain.CompressLevel;
import com.setec.resource.domain.File;
import com.setec.resource.domain.FileType;
import com.setec.resource.domain.ResizePreset;
import com.setec.resource.feature.file.dto.*;
import com.setec.resource.feature.minio.MinioService;
import com.setec.resource.util.FileCompressUtil;
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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final MinioService minioService;
    private final FileRepository fileRepository;
    private final MinioClient minioClient;

    @Value("${media.base-uri}")
    private String baseUri;

    @Value("${media.image-end-point}")
    private String imageEndpoint;

    @Value("${minio.bucket-name}")
    String bucketName;

    @Override
    public FileResponse uploadSingleFile(MultipartFile file, boolean compress, CompressLevel level, FileType type, ResizePreset preset,int w,int h) {

        String contentType = file.getContentType();
        String folderName = getValidFolder(file);
        String originalExtension = MediaUtil.extractExtension(Objects.requireNonNull(file.getOriginalFilename()));

        // Only process images. Note: Thumbnails does not support WebP natively without extra plugins.
        boolean isImage = contentType != null && contentType.startsWith("image/");
        boolean canProcess = isImage && !originalExtension.equalsIgnoreCase("webp");

        String newName;
        do {
            newName = UUID.randomUUID().toString();
        } while (fileRepository.existsByFileName(newName + "." + originalExtension));

        String objectName = folderName + "/" + newName + "." + originalExtension;
        long size = file.getSize();
        InputStream inputStream = null;

        try {
            inputStream = file.getInputStream();

            // 1. BETTER WAY: Single pass processing for Resize AND Compression
            if (canProcess && (compress || (preset != null && preset != ResizePreset.ORIGINAL))) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(inputStream);

                // Handle Resizing
                if (preset != null && preset != ResizePreset.ORIGINAL) {
                    if(h>0&&w>0){
                        builder.size(w, h).keepAspectRatio(true);
                    }else{
                        builder.size(preset.getWidth(), preset.getHeight()).keepAspectRatio(true);
                    }

                } else {
                    builder.scale(1.0);
                }

                // Handle Compression
                if (compress) {
                    double quality = FileCompressUtil.getCompressValue(level);
                    builder.outputQuality(quality);
                }

                builder.toOutputStream(baos);

                byte[] processedBytes = baos.toByteArray();
                size = processedBytes.length;
                inputStream.close(); // Close original
                inputStream = new ByteArrayInputStream(processedBytes); // Replace with processed
            }

            // 2. Upload to Minio
            minioService.uploadFile(inputStream, size, contentType, objectName);

        } catch (Exception e) {
            log.error("Upload failed for file {}: {}", originalExtension, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File processing error");
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
        }

        // 3. Store Metadata
        File fileObject = new File();
        fileObject.setFileName(newName + "." + originalExtension);
        fileObject.setFileSize(size);
        fileObject.setContentType(contentType);
        fileObject.setFolder(folderName);
        fileObject.setExtension(originalExtension);
        fileObject.setType(type);
        fileRepository.save(fileObject);

        return FileResponse.builder()
                .name(newName + "." + originalExtension)
                .contentType(contentType)
                .extension(originalExtension)
                .size(size)
                .type(type)
                .uri(baseUri + imageEndpoint + "/view/" + newName + "." + originalExtension)
                .build();
    }

    @Override
    public FileResponse uploadSingleFile(MultipartFile file, boolean compress, CompressLevel level, FileType type) {
        return uploadSingleFile(file, compress, level, type, ResizePreset.ORIGINAL,0,0);
    }

    @Override
    public FileResponse uploadSingleFile(MultipartFile file) {
        return uploadSingleFile(file, false, CompressLevel.LOW, FileType.DEFAULT, ResizePreset.ORIGINAL,0,0);
    }

    @Override
    public List<FileResponse> loadAllFiles() {
        return fileRepository.findAll().stream().map(file -> FileResponse.builder()
                .name(file.getFileName())
                .contentType(file.getContentType())
                .extension(file.getExtension())
                .size(file.getFileSize())
                .type(file.getType())
                .uri(baseUri + imageEndpoint + "/view/" + file.getFileName())
                .build()).toList();
    }

    @Override
    public void delete(List<FileDeleteRequest> fileDeleteRequests) {
        fileDeleteRequests.forEach(req -> deleteFileByName(req.fileName()));
    }

    @Override
    public List<FileNameResponse> getAllFileNames() {
        return fileRepository.findAllFileNames();
    }

    @Override
    public FileResponse loadFileByName(String fileName) {
        File file = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return FileResponse.builder()
                .name(fileName)
                .contentType(file.getContentType())
                .extension(file.getExtension())
                .uri(baseUri + imageEndpoint + "/view/" + fileName)
                .build();
    }

    @Override
    public void deleteFileByName(String fileName) {
        File file = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            minioService.deleteFile(file.getFolder() + "/" + fileName);
            fileRepository.delete(file);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delete failed");
        }
    }

    @Override
    public Resource downloadFileByName(String mediaName) {
        try {
            File file = fileRepository.findByFileName(mediaName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            InputStream inputStream = minioService.getFile(file.getFolder() + "/" + mediaName);
            Path tempFile = Files.createTempFile("minio-down-", mediaName);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return new UrlResource(tempFile.toUri());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Download failed");
        }
    }

    @Override
    public Resource viewFileRange(String fileName, String rangeHeader) {
        File fileMetadata = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        long fileSize = fileMetadata.getFileSize();
        long start = 0;
        long end = fileSize - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) end = Long.parseLong(ranges[1]);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
        try {
            return new InputStreamResource(minioService.getFile(fileMetadata.getFolder() + "/" + fileName, start, (end - start) + 1));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Range view failed");
        }
    }

    @Override
    public FileStreamResponse getFileStream(String fileName, String rangeHeader) {
        File fileMetadata = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        long fileSize = fileMetadata.getFileSize();
        long start = 0, end = fileSize - 1;
        boolean isPartial = rangeHeader != null;

        if (isPartial && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty()) end = Math.min(Long.parseLong(ranges[1]), fileSize - 1);
        }

        try {
            InputStream inputStream = minioService.getFile(fileMetadata.getFolder() + "/" + fileName, start, (end - start) + 1);
            return new FileStreamResponse(new InputStreamResource(inputStream), fileMetadata.getContentType(), fileSize, start, end, isPartial);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stream failed");
        }
    }

    @Override
    public FileViewResponse getBackground(String type) {
        File file = fileRepository.findOneRandomByType(type);
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(file.getFolder() + "/" + file.getFileName()).build());
            return FileViewResponse.builder()
                    .fileName(file.getFileName())
                    .fileSize(file.getFileSize())
                    .contentType(file.getContentType())
                    .stream(new InputStreamResource(inputStream))
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Background fetch failed");
        }
    }

    @Override
    public FileViewResponse getBackgroundSmooth(String type) {
        File file = fileRepository.findOneRandomByType(type);
        try {
            InputStream originalStream = minioService.getFile(file.getFolder() + "/" + file.getFileName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(originalStream).scale(1.0).outputFormat("jpg").toOutputStream(baos);
            byte[] progressiveBytes = convertToProgressive(baos.toByteArray());
            return FileViewResponse.builder()
                    .fileName(file.getFileName())
                    .fileSize((long) progressiveBytes.length)
                    .contentType("image/jpeg")
                    .stream(new InputStreamResource(new ByteArrayInputStream(progressiveBytes)))
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Smooth process failed");
        }
    }

    private byte[] convertToProgressive(byte[] imageData) throws IOException {
        var image = ImageIO.read(new ByteArrayInputStream(imageData));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
        }
        writer.dispose();
        return out.toByteArray();
    }

    private static String getValidFolder(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("video/") || contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");
        }
        return contentType.split("/")[0];
    }
}