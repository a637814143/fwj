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
import java.util.stream.Collectors;

@Service
public class ImageStorageService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".png", ".gif", ".webp");
    private static final Set<String> CERTIFICATE_EXTENSIONS = Set.of(".jpg", ".png", ".gif", ".webp", ".pdf");

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            ".jpg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif",
            ".webp", "image/webp",
            ".pdf", "application/pdf"
    );

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "application/pdf", ".pdf"
    );

    private final Path imageLocation;
    private final long imageMaxFileSize;
    private final Path certificateLocation;
    private final long certificateMaxFileSize;

    public ImageStorageService(
            @Value("${app.image-upload.dir:uploads/house-images}") String imageDir,
            @Value("${app.image-upload.max-size-bytes:5242880}") long imageMaxFileSize,
            @Value("${app.certificate-upload.dir:uploads/property-certificates}") String certificateDir,
            @Value("${app.certificate-upload.max-size-bytes:7340032}") long certificateMaxFileSize
    ) {
        this.imageLocation = initialiseDirectory(imageDir);
        this.imageMaxFileSize = imageMaxFileSize > 0 ? imageMaxFileSize : 5 * 1024 * 1024;
        this.certificateLocation = initialiseDirectory(certificateDir);
        this.certificateMaxFileSize = certificateMaxFileSize > 0 ? certificateMaxFileSize : this.imageMaxFileSize;
    }

    private Path initialiseDirectory(String directory) {
        Path path = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize storage directory", exception);
        }
        return path;
    }

    public String store(MultipartFile file) {
        return storeFile(file, imageLocation, imageMaxFileSize, IMAGE_EXTENSIONS);
    }

    public Resource loadAsResource(String filename) {
        return loadResource(filename, imageLocation, "image");
    }

    public MediaType detectMediaType(String filename) {
        return detectMediaType(filename, imageLocation, IMAGE_EXTENSIONS);
    }

    public String storeCertificate(MultipartFile file) {
        return storeFile(file, certificateLocation, certificateMaxFileSize, CERTIFICATE_EXTENSIONS);
    }

    public Resource loadCertificateAsResource(String filename) {
        return loadResource(filename, certificateLocation, "certificate");
    }

    public MediaType detectCertificateMediaType(String filename) {
        return detectMediaType(filename, certificateLocation, CERTIFICATE_EXTENSIONS);
    }

    private String storeFile(MultipartFile file,
                             Path targetDirectory,
                             long maxFileSize,
                             Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty.");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file exceeds the maximum allowed size.");
        }
        String extension = resolveExtension(file, allowedExtensions)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, buildUnsupportedMessage(allowedExtensions)));

        String filename = UUID.randomUUID().toString().replaceAll("-", "") + extension;
        Path destination = targetDirectory.resolve(filename).normalize();
        if (!destination.startsWith(targetDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot store file outside of the configured directory.");
        }
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file.", exception);
        }
        return filename;
    }

    private Resource loadResource(String filename, Path rootLocation, String resourceLabel) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, resourceLabel + " name is required.");
        }
        Path filePath = rootLocation.resolve(filename).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + resourceLabel + " path requested.");
        }
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, resourceLabel + " not found.");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, resourceLabel + " not found.");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, resourceLabel + " not found.", exception);
        }
    }

    private MediaType detectMediaType(String filename, Path rootLocation, Set<String> allowedExtensions) {
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
        return resolveExtension(filename, allowedExtensions)
                .map(ext -> CONTENT_TYPE_BY_EXTENSION.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .map(MediaType::parseMediaType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Optional<String> resolveExtension(MultipartFile file, Set<String> allowedExtensions) {
        Optional<String> extensionFromName = resolveExtension(file.getOriginalFilename(), allowedExtensions);
        if (extensionFromName.isPresent()) {
            return extensionFromName;
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT));
            if (normalized != null && allowedExtensions.contains(normalized)) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolveExtension(String filename, Set<String> allowedExtensions) {
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
        return allowedExtensions.contains(normalized) ? Optional.of(normalized) : Optional.empty();
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

    private String buildUnsupportedMessage(Set<String> allowedExtensions) {
        String allowedList = allowedExtensions.stream()
                .map(ext -> ext.substring(1).toUpperCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.joining(", "));
        return "Unsupported file format. Allowed types: " + allowedList + ".";
    }
}

