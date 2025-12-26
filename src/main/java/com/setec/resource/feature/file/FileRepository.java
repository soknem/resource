package com.setec.resource.feature.file;


import com.setec.resource.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long>{

    Optional<File> findByFileName(String fileName);

    boolean existsByFileName(String fileName);
}