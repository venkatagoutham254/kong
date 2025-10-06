/*package aforo.kong;

import aforo.kong.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class KongIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    public void testUsageIngestion() throws Exception {
        // Create a sample Kong event
        KongEventDTO event = new KongEventDTO();
        event.setCorrelationId("test-" + System.currentTimeMillis());
        event.setTimestamp(Instant.now());
        
        KongEventDTO.ServiceInfo service = new KongEventDTO.ServiceInfo();
        service.setId("service-1");
        service.setName("orders-api");
        event.setService(service);
        
        KongEventDTO.ConsumerInfo consumer = new KongEventDTO.ConsumerInfo();
        consumer.setId("consumer-1");
        consumer.setUsername("test-user");
        event.setConsumer(consumer);
        
        KongEventDTO.RequestInfo request = new KongEventDTO.RequestInfo();
        request.setMethod("GET");
        request.setPath("/v1/orders");
        request.setSize(256L);
        event.setRequest(request);
        
        KongEventDTO.ResponseInfo response = new KongEventDTO.ResponseInfo();
        response.setStatus(200);
        response.setSize(1024L);
        event.setResponse(response);
        
        KongEventDTO.LatenciesInfo latencies = new KongEventDTO.LatenciesInfo();
        latencies.setRequest(150L);
        latencies.setKong(5L);
        latencies.setProxy(145L);
        event.setLatencies(latencies);
        
        // Send to ingestion endpoint (no auth required)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Organization-Id", "1");
        
        HttpEntity<KongEventDTO> entity = new HttpEntity<>(event, headers);
        
        ResponseEntity<Void> result = restTemplate.exchange(
            "/integrations/kong/ingest",
            HttpMethod.POST,
            entity,
            Void.class
        );
        
        // Verify accepted
        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
    }
    
    @Test
    public void testBatchIngestion() throws Exception {
        // Create multiple events
        List<KongEventDTO> events = List.of(
            createSampleEvent("batch-1"),
            createSampleEvent("batch-2"),
            createSampleEvent("batch-3")
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Organization-Id", "1");
        
        HttpEntity<List<KongEventDTO>> entity = new HttpEntity<>(events, headers);
        
        ResponseEntity<Void> result = restTemplate.exchange(
            "/integrations/kong/ingest",
            HttpMethod.POST,
            entity,
            Void.class
        );
        
        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
    }
    
    @Test
    public void testEnforceRateLimits() throws Exception {
        EnforceGroupsRequestDTO request = new EnforceGroupsRequestDTO();
        
        EnforceGroupsRequestDTO.PlanGroupMapping mapping = new EnforceGroupsRequestDTO.PlanGroupMapping();
        mapping.setPlanId("bronze");
        mapping.setConsumerGroupName("bronze-tier");
        
        EnforceGroupsRequestDTO.RateLimit dailyLimit = new EnforceGroupsRequestDTO.RateLimit();
        dailyLimit.setWindow("day");
        dailyLimit.setLimit(1000L);
        
        mapping.setLimits(List.of(dailyLimit));
        request.setMappings(List.of(mapping));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("test-token");
        headers.set("X-Organization-Id", "1");
        
        HttpEntity<EnforceGroupsRequestDTO> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Void> result = restTemplate.exchange(
            "/integrations/kong/enforce/groups",
            HttpMethod.POST,
            entity,
            Void.class
        );
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    public void testSuspendConsumer() throws Exception {
        SuspendRequestDTO request = new SuspendRequestDTO();
        request.setConsumerId("consumer-test");
        request.setMode("group");
        request.setReason("Exceeded quota");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("test-token");
        headers.set("X-Organization-Id", "1");
        
        HttpEntity<SuspendRequestDTO> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Void> result = restTemplate.exchange(
            "/integrations/kong/suspend",
            HttpMethod.POST,
            entity,
            Void.class
        );
        
        // Should be NOT_FOUND if consumer doesn't exist
        assertTrue(result.getStatusCode() == HttpStatus.NOT_FOUND || 
                  result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    public void testHealthCheck() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-token");
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> result = restTemplate.exchange(
            "/integrations/kong/health",
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue((Boolean) result.getBody().get("kongReachable"));
    }
    
    private KongEventDTO createSampleEvent(String correlationId) {
        KongEventDTO event = new KongEventDTO();
        event.setCorrelationId(correlationId);
        event.setTimestamp(Instant.now());
        
        KongEventDTO.ServiceInfo service = new KongEventDTO.ServiceInfo();
        service.setId("service-1");
        service.setName("test-service");
        event.setService(service);
        
        KongEventDTO.RequestInfo request = new KongEventDTO.RequestInfo();
        request.setMethod("GET");
        request.setPath("/test");
        event.setRequest(request);
        
        KongEventDTO.ResponseInfo response = new KongEventDTO.ResponseInfo();
        response.setStatus(200);
        event.setResponse(response);
        
        return event;
    }
}
*/