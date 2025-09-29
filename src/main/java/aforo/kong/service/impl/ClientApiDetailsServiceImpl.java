package aforo.kong.service.impl;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.mapper.ClientApiDetailsMapper;
import aforo.kong.repository.ClientApiDetailsRepository;
import aforo.kong.service.ClientApiDetailsService;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.List;

@Service
public class ClientApiDetailsServiceImpl implements ClientApiDetailsService {

    private final RestTemplate restTemplate;
    private final ClientApiDetailsRepository repository;
    private final ClientApiDetailsMapper mapper;

    public ClientApiDetailsServiceImpl(RestTemplate restTemplate,
                                       ClientApiDetailsRepository repository,
                                       ClientApiDetailsMapper mapper) {
        this.restTemplate = restTemplate;
        this.repository = repository;
        this.mapper = mapper;
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
    
        // Build full URL
        String fullUrl = clientApiDetails.getBaseUrl() + clientApiDetails.getEndpoint();
    
        // Call external API with Authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(clientApiDetails.getAuthToken());
        // Optional: organization header if configured
        if (clientApiDetails.getName() != null && clientApiDetails.getName().startsWith("org:")) {
            headers.set("Kong-Admin-Organization", clientApiDetails.getName().substring(4));
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
    
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                String.class
        );
    
        // Map DB entity → DTO
        ClientApiDetailsDTO dto = mapper.toDto(clientApiDetails);
        dto.setResponseBody(response.getBody()); // only if you added responseBody field
    
        return dto;
    }
    

}
