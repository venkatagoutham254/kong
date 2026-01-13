package aforo.kong.dto.konnect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonnectConnectionResponseDTO {
    private Long connectionId;
    private String status;
    private String controlPlaneId;
    private String message;
}
