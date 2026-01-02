package com.setec.resource.feature.file;

import com.setec.resource.domain.CompressLevel;
import com.setec.resource.domain.File;
import com.setec.resource.feature.file.dto.FileResponse;
import com.setec.resource.feature.file.dto.FileStreamResponse;
import com.setec.resource.feature.file.dto.FileViewResponse;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileRepository fileRepository;


    @ResponseStatus(HttpStatus.CREATED)
//    @PreAuthorize("hasAnyAuthority('file:write')")
    @PostMapping(value = "", consumes = "multipart/form-data")
    FileResponse uploadFile(@RequestPart MultipartFile file,
                            @RequestParam(defaultValue = "false") boolean compress,
                            @RequestParam(defaultValue = "NONE")CompressLevel level

                            ) {

        return fileService.uploadSingleFile(file, compress,level);
    }


    @GetMapping()
//    @PreAuthorize("hasAnyAuthority('file:read')")
    List<FileResponse> loadAllFile() {
        return fileService.loadAllFiles();
    }


    @GetMapping("/{fileName}")
//    @PreAuthorize("hasAnyAuthority('file:read')")
    FileResponse loadFileByName(@PathVariable String fileName) {
        return fileService.loadFileByName(fileName);
    }


    //    @PreAuthorize("hasAnyAuthority('file:delete')")
    @DeleteMapping("/{fileName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFileByName(@PathVariable String fileName) {
        fileService.deleteFileByName(fileName);
    }

    // produces = Accept
    // consumes = Content-Type
    @GetMapping(path = "/download/{fileName}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<?> downloadFileByName(@PathVariable String fileName) {

        System.out.println("Start download");

        Resource resource = fileService.downloadFileByName(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @GetMapping(value = "/view/{fileName}")
    public ResponseEntity<Resource> viewByFileName(
            @PathVariable String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        FileStreamResponse stream = fileService.getFileStream(fileName, rangeHeader);

        // Case 1: Standard 200 OK (Full File)
        if (!stream.isPartial()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(stream.contentType()))
                    .contentLength(stream.fileSize())
                    .body(stream.resource());
        }

        // Case 2: 206 Partial Content (Video Seeking)
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(stream.contentType()))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + stream.start() + "-" + stream.end() + "/" + stream.fileSize())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength((stream.end() - stream.start()) + 1)
                .body(stream.resource());
    }
}