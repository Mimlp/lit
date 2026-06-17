package com.litsite.lit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload-dir:./uploads/avatars}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию для загрузки: " + uploadDir, e);
        }
    }

    public String saveAvatar(MultipartFile file, Long userId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String filename = "avatar_" + userId + "_" + UUID.randomUUID() + extension;
        Path filePath = Path.of(uploadDir, filename);

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return baseUrl + "/api/avatars/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при сохранении файла", e
            );
        }
    }

    public ResponseEntity<Resource> serveAvatar(String filename) {
        try {
            Path filePath = Path.of(uploadDir, filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Файл не найден"
                );
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Можно определить динамически
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Неверный путь к файлу", e
            );
        }
    }

    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return;

        try {
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
            Path filePath = Path.of(uploadDir, filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Логируем, но не прерываем выполнение
            System.err.println("Не удалось удалить файл аватара: " + avatarUrl);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Файл не может быть пустым"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Разрешены только изображения"
            );
        }

        long maxSize = 5 * 1024 * 1024; // 5 MB
        if (file.getSize() > maxSize) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Максимальный размер файла: 5 MB"
            );
        }
    }
}
