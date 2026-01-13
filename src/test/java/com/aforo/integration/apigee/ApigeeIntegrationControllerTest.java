package com.aforo.integration.apigee;

import com.aforo.integration.apigee.api.ApigeeIntegrationController;
import com.aforo.integration.apigee.dto.*;
import com.aforo.integration.apigee.service.ApigeeIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Apigee Integration Controller.
 */
@ExtendWith(MockitoExtension.class)
class ApigeeIntegrationControllerTest {
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @Mock
    private ApigeeIntegrationService apigeeIntegrationService;
    
    @InjectMocks
    private ApigeeIntegrationController controller;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testConnect_Success() throws Exception {
        // Given
        ApigeeConnectRequest request = ApigeeConnectRequest.builder()
            .org("test-org")
            .env("test")
            .build();
        
        ApigeeConnectResponse response = ApigeeConnectResponse.builder()
            .status("connected")
            .org("test-org")
            .env("test")
            .message("Successfully connected")
            .apiProxyCount(5)
            .apiProductCount(3)
            .developerCount(10)
            .build();
        
        when(apigeeIntegrationService.testConnection(any())).thenReturn(Mono.just(response));
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/connect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("connected"))
            .andExpect(jsonPath("$.org").value("test-org"));
    }
    
    @Test
    void testCatalogSync_Success() throws Exception {
        // Given
        ApigeeCatalogSyncResponse response = ApigeeCatalogSyncResponse.builder()
            .status("COMPLETED")
            .productsImported(5)
            .endpointsImported(10)
            .customersImported(15)
            .appsImported(20)
            .build();
        
        when(apigeeIntegrationService.syncCatalog(any())).thenReturn(Mono.just(response));
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/catalog/sync"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.productsImported").value(5));
    }
    
    @Test
    void testIngestEvents_SingleEvent() throws Exception {
        // Given
        ApigeeEvent event = ApigeeEvent.builder()
            .timestamp("2025-12-05T10:00:00Z")
            .org("test-org")
            .env("test")
            .apiProxy("payment-api")
            .method("POST")
            .status(200)
            .build();
        
        when(apigeeIntegrationService.ingestEvents(any())).thenReturn(Mono.just(1));
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("accepted"));
    }
    
    @Test
    void testIngestEvents_MultipleEvents() throws Exception {
        // Given
        List<ApigeeEvent> events = List.of(
            ApigeeEvent.builder()
                .timestamp("2025-12-05T10:00:00Z")
                .org("test-org")
                .env("test")
                .apiProxy("payment-api")
                .build(),
            ApigeeEvent.builder()
                .timestamp("2025-12-05T10:01:00Z")
                .org("test-org")
                .env("test")
                .apiProxy("user-api")
                .build()
        );
        
        when(apigeeIntegrationService.ingestEvents(any())).thenReturn(Mono.just(2));
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("accepted"))
            .andExpect(jsonPath("$.eventsProcessed").value(2));
    }
    
    @Test
    void testEnforcePlans_Success() throws Exception {
        // Given
        ApigeeEnforcePlanRequest request = ApigeeEnforcePlanRequest.builder()
            .mappings(List.of(
                ApigeeEnforcePlanRequest.PlanMapping.builder()
                    .planId("SILVER")
                    .developerId("dev@example.com")
                    .appName("test-app")
                    .consumerKey("test-key")
                    .apiProductName("SILVER_PRODUCT")
                    .build()
            ))
            .build();
        
        List<Map<String, Object>> results = List.of(
            Map.of("planId", "SILVER", "status", "success")
        );
        
        when(apigeeIntegrationService.enforcePlans(any())).thenReturn(Mono.just(results));
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/enforce/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));
    }
    
    @Test
    void testSuspendApp_Success() throws Exception {
        // Given
        ApigeeSuspendRequest request = ApigeeSuspendRequest.builder()
            .developerId("dev@example.com")
            .appName("test-app")
            .consumerKey("test-key")
            .mode("revoke")
            .reason("wallet_zero")
            .build();
        
        when(apigeeIntegrationService.suspendApp(any())).thenReturn(Mono.empty());
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("suspended"));
    }
    
    @Test
    void testResumeApp_Success() throws Exception {
        // Given
        when(apigeeIntegrationService.resumeApp(any(), any(), any())).thenReturn(Mono.empty());
        
        // When & Then
        mockMvc.perform(post("/integrations/apigee/resume")
                .param("developerId", "dev@example.com")
                .param("appName", "test-app")
                .param("consumerKey", "test-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("resumed"));
    }
    
    @Test
    void testHealth_Success() throws Exception {
        // Given
        Map<String, Object> health = Map.of(
            "apigeeReachable", true,
            "org", "test-org",
            "env", "test"
        );
        
        when(apigeeIntegrationService.checkHealth()).thenReturn(Mono.just(health));
        
        // When & Then
        mockMvc.perform(get("/integrations/apigee/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apigeeReachable").value(true))
            .andExpect(jsonPath("$.org").value("test-org"));
    }
}
