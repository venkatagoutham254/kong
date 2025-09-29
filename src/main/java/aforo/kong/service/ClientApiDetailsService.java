package aforo.kong.service;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.entity.ClientApiDetails;

import java.util.List;

public interface ClientApiDetailsService {
    ClientApiDetailsDTO fetchApiDetails(String clientApiUrl);
    List<ClientApiDetails> getAllApiDetails();
    ClientApiDetails getApiDetailsById(Long id);
    void deleteApiDetailsById(Long id);
    ClientApiDetails saveClientApiDetails(ClientApiDetailsDTO dto);
    ClientApiDetailsDTO fetchApiDetailsFromDB(Long id);
}
