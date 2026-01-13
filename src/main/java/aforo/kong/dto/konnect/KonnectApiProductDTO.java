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
public class KonnectApiProductDTO {
    private String konnectApiProductId;
    private String name;
    private String description;
    private String status;
    private Integer versionsCount;
    private Instant updatedAt;
}
