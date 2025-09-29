package aforo.kong.mapper;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.entity.ClientApiDetails;
import org.springframework.stereotype.Component;

@Component
public class ClientApiDetailsMapperImpl implements ClientApiDetailsMapper {
    @Override
    public ClientApiDetails toEntity(ClientApiDetailsDTO dto) {
        if (dto == null) return null;
        ClientApiDetails entity = new ClientApiDetails();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setEndpoint(dto.getEndpoint());
                // Strip optional "Bearer " prefix so we don't double-prefix later
        if (dto.getAuthToken() != null && dto.getAuthToken().startsWith("Bearer ")) {
            entity.setAuthToken(dto.getAuthToken().substring(7));
        } else {
            entity.setAuthToken(dto.getAuthToken());
        }
        return entity;
    }

    @Override
    public ClientApiDetailsDTO toDto(ClientApiDetails entity) {
        if (entity == null) return null;
        ClientApiDetailsDTO dto = new ClientApiDetailsDTO();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setEndpoint(entity.getEndpoint());
                dto.setAuthToken("Bearer " + entity.getAuthToken());
        return dto;
    }
}
