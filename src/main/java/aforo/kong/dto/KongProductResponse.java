package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KongProductResponse {
  private List<KongProductDTO> data;
  private Map<String,Object> meta; // keep generic
}

