package aforo.kong.service;

import aforo.kong.dto.konnect.*;

import java.util.List;

public interface KonnectService {
    KonnectConnectionResponseDTO createOrUpdateConnection(Long orgId, KonnectConnectionRequestDTO request);
    KonnectTestResponseDTO testConnection(Long orgId);
    List<KonnectApiProductDTO> fetchApiProducts(Long orgId);
    KonnectImportResponseDTO importApiProducts(Long orgId, KonnectImportRequestDTO request);
    List<KonnectImportedProductDTO> listImportedProducts(Long orgId);
    KonnectSyncPreviewDTO previewSync(Long orgId);
    KonnectImportResponseDTO applySync(Long orgId);
    void autoRefresh();
}
