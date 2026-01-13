package aforo.kong.dto.konnect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonnectImportResponseDTO {
    private Integer imported;
    private Integer updated;
    private Integer failed;
    private List<ImportedItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportedItemDTO {
        private String konnectApiProductId;
        private Long aforoProductId;
        private String action;
    }
}
