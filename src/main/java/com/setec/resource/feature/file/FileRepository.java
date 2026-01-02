package com.setec.resource.feature.file;


import com.setec.resource.domain.File;
import com.setec.resource.feature.file.dto.FileNameResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long>{

    Optional<File> findByFileName(String fileName);

    boolean existsByFileName(String fileName);

    @Query("select f.fileName from File f")
    List<FileNameResponse> findAllFileNames();
}