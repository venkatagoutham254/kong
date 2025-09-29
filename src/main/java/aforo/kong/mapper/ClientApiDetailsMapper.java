package aforo.kong.mapper;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.entity.ClientApiDetails;

public interface ClientApiDetailsMapper {
    ClientApiDetails toEntity(ClientApiDetailsDTO dto);
    ClientApiDetailsDTO toDto(ClientApiDetails entity);
}
