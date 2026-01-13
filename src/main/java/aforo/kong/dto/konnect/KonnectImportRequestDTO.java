package aforo.kong.dto.konnect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KonnectImportRequestDTO {
    @JsonProperty("api_product_ids")
    private List<String> selectedApiProductIds;
}
