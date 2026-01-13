package aforo.kong.dto.konnect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonnectTestResponseDTO {
    private Boolean ok;
    private String message;
    private Map<String, Integer> counts;
}
