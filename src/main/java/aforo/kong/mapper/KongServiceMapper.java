package aforo.kong.mapper;

import aforo.kong.dto.KongServiceDTO;
import aforo.kong.entity.KongService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface KongServiceMapper {
    
    @Mapping(target = "internalId", ignore = true)
    @Mapping(target = "routes", ignore = true)
    @Mapping(target = "tags", ignore = true) // Will be handled manually as JSON
    @Mapping(target = "caCertificates", ignore = true) // Will be handled manually as JSON
    @Mapping(target = "clientCertificate", ignore = true) // Will be handled manually as JSON
    KongService toEntity(KongServiceDTO dto);
    
    @Mapping(target = "tags", ignore = true) // Will be handled manually from JSON
    @Mapping(target = "caCertificates", ignore = true) // Will be handled manually from JSON
    @Mapping(target = "clientCertificate", ignore = true) // Will be handled manually from JSON
    KongServiceDTO toDto(KongService entity);
    
    @Mapping(target = "internalId", ignore = true)
    @Mapping(target = "routes", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "caCertificates", ignore = true)
    @Mapping(target = "clientCertificate", ignore = true)
    void updateEntityFromDto(KongServiceDTO dto, @MappingTarget KongService entity);
}
