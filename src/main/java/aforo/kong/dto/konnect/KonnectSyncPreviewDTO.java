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
public class KonnectSyncPreviewDTO {
    private List<KonnectApiProductDTO> added;
    private List<KonnectApiProductDTO> removed;
    private List<KonnectApiProductDTO> changed;
}
