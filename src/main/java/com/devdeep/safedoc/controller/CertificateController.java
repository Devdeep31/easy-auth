package com.devdeep.safedoc.controller;


import com.devdeep.safedoc.dto.CertificateDTO;
import com.devdeep.safedoc.dto.CertificateRequest;
import com.devdeep.safedoc.entity.Certificate;
import com.devdeep.safedoc.service.CertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issueCertificate(
            @RequestPart("request") CertificateRequest request,
            @RequestPart("document") MultipartFile document) {

        try {
            CertificateDTO certificateDTO = certificateService.issueCertificate(request, document);
            return ResponseEntity.ok(certificateDTO);
        } catch (RuntimeException ex) {
            // You can log it here if needed
            if (ex.getMessage().contains("Blockchain unavailable")) {
                // Send custom response to inform the user
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Blockchain is currently unavailable. Your certificate request has been queued.");
            } else {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred: " + ex.getMessage());
            }
        }
    }


}
