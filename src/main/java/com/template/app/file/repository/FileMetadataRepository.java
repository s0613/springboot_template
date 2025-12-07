package com.template.app.file.repository;

import com.template.app.file.domain.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByStoredFilename(String storedFilename);

    @Query("SELECT f FROM FileMetadata f WHERE f.uploaderId = :uploaderId AND f.deletedAt IS NULL ORDER BY f.createdAt DESC")
    Page<FileMetadata> findByUploaderIdAndNotDeleted(@Param("uploaderId") Long uploaderId, Pageable pageable);

    @Query("SELECT f FROM FileMetadata f WHERE f.referenceType = :referenceType AND f.referenceId = :referenceId AND f.deletedAt IS NULL")
    List<FileMetadata> findByReference(@Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    @Query("SELECT f FROM FileMetadata f WHERE f.fileCategory = :category AND f.uploaderId = :uploaderId AND f.deletedAt IS NULL")
    List<FileMetadata> findByCategoryAndUploader(@Param("category") FileMetadata.FileCategory category, @Param("uploaderId") Long uploaderId);

    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.uploaderId = :uploaderId AND f.deletedAt IS NULL")
    Long getTotalFileSizeByUploader(@Param("uploaderId") Long uploaderId);

    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.uploaderId = :uploaderId AND f.deletedAt IS NULL")
    Long countByUploader(@Param("uploaderId") Long uploaderId);
}
