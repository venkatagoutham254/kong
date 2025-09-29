package aforo.kong.service.impl;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.mapper.ClientApiDetailsMapper;
import aforo.kong.repository.ClientApiDetailsRepository;
import aforo.kong.service.ClientApiDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Service
public class ClientApiDetailsServiceImpl implements ClientApiDetailsService {

    private final ClientApiDetailsRepository repository;
    private final ClientApiDetailsMapper mapper;
    private final RestTemplate restTemplate;

    public ClientApiDetailsServiceImpl(ClientApiDetailsRepository repository, ClientApiDetailsMapper mapper, RestTemplate restTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public ClientApiDetailsDTO fetchApiDetails(String clientApiUrl) {
        ResponseEntity<ClientApiDetailsDTO> response = restTemplate.getForEntity(
                clientApiUrl,
                ClientApiDetailsDTO.class
        );
        ClientApiDetailsDTO dto = response.getBody();

        if (dto == null) {
            throw new RuntimeException("Client API details response body is null");
        }

        // Set default values if name and description are null
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            dto.setName("Kong API Client");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            dto.setDescription("Auto-generated Kong API client configuration");
        }
        
        ClientApiDetails entity = mapper.toEntity(dto);
        repository.save(entity);

        return dto;
    }

    @Override
    public List<ClientApiDetails> getAllApiDetails() {
        return repository.findAll();
    }

    @Override
    public ClientApiDetails getApiDetailsById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ClientApiDetails not found for ID: " + id));
    }

    @Override
    public void deleteApiDetailsById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public ClientApiDetails saveClientApiDetails(ClientApiDetailsDTO dto) {
        ClientApiDetails entity = mapper.toEntity(dto);
        return repository.save(entity);
    }

    @Override
    public ClientApiDetailsDTO fetchApiDetailsFromDB(Long id) {
        ClientApiDetails clientApiDetails = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ClientApiDetails not found for ID: " + id));
    
        // Just return the stored API details (no need to call external API)
        ClientApiDetailsDTO dto = mapper.toDto(clientApiDetails);
        return dto;
    }
}
