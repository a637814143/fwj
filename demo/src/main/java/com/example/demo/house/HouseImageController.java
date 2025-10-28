package com.example.demo.house;

import com.example.demo.storage.ImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/houses/images")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HouseImageController {

    private final ImageStorageService storageService;

    public HouseImageController(ImageStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String filename = storageService.store(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/houses/images/")
                .path(filename)
                .toUriString();
        Map<String, String> body = Map.of(
                "filename", filename,
                "url", url
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping(value = "/certificates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadCertificate(@RequestParam("file") MultipartFile file) {
        String filename = storageService.storeCertificate(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/houses/images/certificates/")
                .path(filename)
                .toUriString();
        Map<String, String> body = Map.of(
                "filename", filename,
                "url", url
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        Resource resource = storageService.loadAsResource(filename);
        MediaType mediaType = storageService.detectMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(resource);
    }

    @GetMapping("/certificates/{filename:.+}")
    public ResponseEntity<Resource> serveCertificate(@PathVariable String filename) {
        Resource resource = storageService.loadCertificateAsResource(filename);
        MediaType mediaType = storageService.detectCertificateMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(resource);
    }
}
