package aforo.kong.controller;

import aforo.kong.dto.konnect.*;
import aforo.kong.service.KonnectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/konnect")
public class KonnectController {

    private final KonnectService konnectService;

    public KonnectController(KonnectService konnectService) {
        this.konnectService = konnectService;
    }

    @PostMapping("/connection")
    public ResponseEntity<KonnectConnectionResponseDTO> createOrUpdateConnection(
            @RequestHeader("X-Organization-Id") Long orgId,
            @RequestBody KonnectConnectionRequestDTO request) {
        KonnectConnectionResponseDTO response = konnectService.createOrUpdateConnection(orgId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connection/test")
    public ResponseEntity<KonnectTestResponseDTO> testConnection(
            @RequestHeader("X-Organization-Id") Long orgId) {
        KonnectTestResponseDTO response = konnectService.testConnection(orgId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api-products")
    public ResponseEntity<List<KonnectApiProductDTO>> fetchApiProducts(
            @RequestHeader("X-Organization-Id") Long orgId) {
        List<KonnectApiProductDTO> products = konnectService.fetchApiProducts(orgId);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/api-products/import")
    public ResponseEntity<KonnectImportResponseDTO> importApiProducts(
            @RequestHeader("X-Organization-Id") Long orgId,
            @RequestBody KonnectImportRequestDTO request) {
        KonnectImportResponseDTO response = konnectService.importApiProducts(orgId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api-products/imported")
    public ResponseEntity<List<KonnectImportedProductDTO>> listImportedProducts(
            @RequestHeader("X-Organization-Id") Long orgId) {
        List<KonnectImportedProductDTO> products = konnectService.listImportedProducts(orgId);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/catalog/preview")
    public ResponseEntity<KonnectSyncPreviewDTO> previewSync(
            @RequestHeader("X-Organization-Id") Long orgId) {
        KonnectSyncPreviewDTO preview = konnectService.previewSync(orgId);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/catalog/apply")
    public ResponseEntity<KonnectImportResponseDTO> applySync(
            @RequestHeader("X-Organization-Id") Long orgId) {
        KonnectImportResponseDTO response = konnectService.applySync(orgId);
        return ResponseEntity.ok(response);
    }
}
