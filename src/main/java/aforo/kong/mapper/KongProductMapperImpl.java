package aforo.kong.mapper;

import aforo.kong.dto.KongProductDTO;
import aforo.kong.entity.KongProduct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KongProductMapperImpl implements KongProductMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public KongProduct toEntity(KongProductDTO dto) {
        if (dto == null) return null;

        KongProduct e = new KongProduct();
        e.setId(dto.getId());
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        e.setCreatedAt(dto.getCreatedAt());
        e.setUpdatedAt(dto.getUpdatedAt());
        e.setVersionCount(dto.getVersionCount());
        e.setOrganizationId(dto.getOrganizationId());
        
        // Convert complex objects to JSON strings for storage
        try {
            if (dto.getLabels() != null) {
                e.setLabels(objectMapper.writeValueAsString(dto.getLabels()));
            }
            if (dto.getPublicLabels() != null) {
                e.setPublicLabelsJson(objectMapper.writeValueAsString(dto.getPublicLabels()));
            }
            if (dto.getPortalIds() != null) {
                e.setPortalIdsJson(objectMapper.writeValueAsString(dto.getPortalIds()));
            }
            if (dto.getPortals() != null) {
                e.setPortalsJson(objectMapper.writeValueAsString(dto.getPortals()));
            }
        } catch (Exception ex) {
            System.err.println("Error serializing JSON fields: " + ex.getMessage());
        }
        
        return e;
    }

    @Override
    public KongProductDTO toDto(KongProduct e) {
        if (e == null) return null;

        KongProductDTO dto = new KongProductDTO();
        dto.setInternalId(e.getInternalId());
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setVersionCount(e.getVersionCount());
        dto.setOrganizationId(e.getOrganizationId());
        
        // Convert JSON strings back to objects
        try {
            if (e.getLabels() != null && !e.getLabels().isEmpty()) {
                dto.setLabels(objectMapper.readValue(e.getLabels(), Map.class));
            }
            if (e.getPublicLabelsJson() != null && !e.getPublicLabelsJson().isEmpty()) {
                dto.setPublicLabels(objectMapper.readValue(e.getPublicLabelsJson(), Map.class));
            }
            if (e.getPortalIdsJson() != null && !e.getPortalIdsJson().isEmpty()) {
                dto.setPortalIds(objectMapper.readValue(e.getPortalIdsJson(), List.class));
            }
            if (e.getPortalsJson() != null && !e.getPortalsJson().isEmpty()) {
                dto.setPortals(objectMapper.readValue(e.getPortalsJson(), List.class));
            }
        } catch (Exception ex) {
            System.err.println("Error deserializing JSON fields: " + ex.getMessage());
        }
        
        return dto;
    }
}
