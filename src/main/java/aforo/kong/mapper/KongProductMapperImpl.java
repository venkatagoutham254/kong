package aforo.kong.mapper;

import aforo.kong.dto.KongProductDTO;
import aforo.kong.entity.KongProduct;
import org.springframework.stereotype.Component;

@Component
public class KongProductMapperImpl implements KongProductMapper {

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
        // NOTE: labels/public_labels/portal_ids/portals are not persisted
        return e;
    }

    @Override
    public KongProductDTO toDto(KongProduct e) {
        if (e == null) return null;

        KongProductDTO dto = new KongProductDTO();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setVersionCount(e.getVersionCount());
        // DTO’s labels/public_labels/... left null
        return dto;
    }
}
