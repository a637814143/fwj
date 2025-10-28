package com.example.demo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".png", ".gif", ".webp");

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            ".jpg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif",
            ".webp", "image/webp"
    );

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final Path rootLocation;
    private final long maxFileSize;

    public ImageStorageService(
            @Value("${app.image-upload.dir:uploads/house-images}") String uploadDir,
            @Value("${app.image-upload.max-size-bytes:5242880}") long maxFileSize
    ) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : 5 * 1024 * 1024;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize image storage directory", exception);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty.");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded image exceeds the maximum allowed size.");
        }
        String extension = resolveExtension(file).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format. Please upload PNG, JPG, GIF or WEBP files."));

        String filename = UUID.randomUUID().toString().replaceAll("-", "") + extension;
        Path destination = rootLocation.resolve(filename).normalize();
        if (!destination.startsWith(rootLocation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot store file outside of the configured directory.");
        }
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded image.", exception);
        }
        return filename;
    }

    public Resource loadAsResource(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image name is required.");
        }
        Path filePath = rootLocation.resolve(filename).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path requested.");
        }
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found.");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found.");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found.", exception);
        }
    }

    public MediaType detectMediaType(String filename) {
        try {
            Path filePath = rootLocation.resolve(filename).normalize();
            if (Files.exists(filePath)) {
                String probed = Files.probeContentType(filePath);
                if (probed != null && !probed.isBlank()) {
                    return MediaType.parseMediaType(probed);
                }
            }
        } catch (IOException | InvalidPathException ignored) {
            // Fall back to extension-based detection
        }
        return resolveExtension(filename)
                .map(ext -> CONTENT_TYPE_BY_EXTENSION.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .map(MediaType::parseMediaType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Optional<String> resolveExtension(MultipartFile file) {
        Optional<String> extensionFromName = resolveExtension(file.getOriginalFilename());
        if (extensionFromName.isPresent()) {
            return extensionFromName;
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT));
            if (normalized != null) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolveExtension(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        String trimmed = filename.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == trimmed.length() - 1) {
            return Optional.empty();
        }
        String rawExtension = trimmed.substring(dotIndex);
        String normalized = normalizeExtension(rawExtension);
        return ALLOWED_EXTENSIONS.contains(normalized) ? Optional.of(normalized) : Optional.empty();
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String lower = extension.toLowerCase(Locale.ROOT);
        if (".jpeg".equals(lower)) {
            return ".jpg";
        }
        return lower;
    }
}
