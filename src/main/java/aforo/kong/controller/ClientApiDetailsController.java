package aforo.kong.controller;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.service.ClientApiDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client-api-details")
public class ClientApiDetailsController {

    private final ClientApiDetailsService clientApiDetailsService;

    public ClientApiDetailsController(ClientApiDetailsService clientApiDetailsService) {
        this.clientApiDetailsService = clientApiDetailsService;
    }

    // Create a new ClientApiDetails
    @PostMapping
    public ResponseEntity<ClientApiDetails> createClientApiDetails(@RequestBody ClientApiDetailsDTO dto) {
        ClientApiDetails savedDetails = clientApiDetailsService.saveClientApiDetails(dto);
        return ResponseEntity.ok(savedDetails);
    }

    // Get all ClientApiDetails
    @GetMapping
    public ResponseEntity<List<ClientApiDetails>> getAllClientApiDetails() {
        List<ClientApiDetails> details = clientApiDetailsService.getAllApiDetails();
        return ResponseEntity.ok(details);
    }

    // Get ClientApiDetails by ID
    @GetMapping("/{id}")
    public ResponseEntity<ClientApiDetails> getClientApiDetailsById(@PathVariable Long id) {
        ClientApiDetails details = clientApiDetailsService.getApiDetailsById(id);
        return ResponseEntity.ok(details);
    }

    // Delete ClientApiDetails by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientApiDetails(@PathVariable Long id) {
        clientApiDetailsService.deleteApiDetailsById(id);
        return ResponseEntity.noContent().build();
    }
}
