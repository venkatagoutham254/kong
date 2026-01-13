package aforo.kong.dto.konnect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonnectImportedProductDTO {
    private Long aforoProductId;
    private String name;
    private String description;
    private String konnectApiProductId;
    private Instant lastSeenAt;
}
