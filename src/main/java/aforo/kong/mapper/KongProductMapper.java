package aforo.kong.mapper;

import aforo.kong.dto.KongProductDTO;
import aforo.kong.entity.KongProduct;

public interface KongProductMapper {
    KongProduct toEntity(KongProductDTO dto);
    KongProductDTO toDto(KongProduct entity);
}
