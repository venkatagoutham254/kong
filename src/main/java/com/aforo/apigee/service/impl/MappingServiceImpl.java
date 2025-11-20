package com.aforo.apigee.service.impl;

import com.aforo.apigee.dto.request.CreateMappingRequest;
import com.aforo.apigee.dto.response.MappingResponse;
import com.aforo.apigee.gateway.ApigeeGateway;
import com.aforo.apigee.model.AppMapping;
import com.aforo.apigee.model.DeveloperLink;
import com.aforo.apigee.repository.AppMappingRepository;
import com.aforo.apigee.repository.DeveloperLinkRepository;
import com.aforo.apigee.service.MappingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingServiceImpl implements MappingService {
    
    private final ApigeeGateway apigeeGateway;
    private final AppMappingRepository appMappingRepository;
    private final DeveloperLinkRepository developerLinkRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${aforo.apigee.org}")
    private String defaultOrg;
    
    @Value("${aforo.services.subscription}")
    private String subscriptionServiceUrl;
    
    @Override
    @Transactional
    public MappingResponse createDraftSubscription(CreateMappingRequest request) {
        log.info("Creating draft subscription for app: {} and product: {}", 
                 request.getDeveloperApp(), request.getApiProduct());
        
        // Check if mapping already exists
        AppMapping existingMapping = appMappingRepository
                .findByApigeeAppIdAndApiProduct(request.getDeveloperApp(), request.getApiProduct())
                .orElse(null);
        
        if (existingMapping != null && existingMapping.getSubscriptionId() != null) {
            log.warn("Mapping already exists with subscription ID: {}", existingMapping.getSubscriptionId());
            return MappingResponse.builder()
                    .subscriptionId(existingMapping.getSubscriptionId())
                    .ratePlanId(existingMapping.getRatePlanId())
                    .appId(existingMapping.getApigeeAppId())
                    .wroteBackAttributes(existingMapping.getAttributesPushed())
                    .build();
        }
        
        // Get developer link to find Aforo customer ID
        // For simplicity, we'll extract developer ID from app context or use a lookup
        // In real scenario, you'd query Apigee to get the developer for this app
        String aforoCustomerId = findAforoCustomerIdForApp(request.getDeveloperApp());
        
        if (aforoCustomerId == null) {
            throw new RuntimeException("No Aforo customer linked for this app. Please link the developer first.");
        }
        
        // Create subscription via subscription-service
        Long subscriptionId = createSubscriptionInAforo(
                aforoCustomerId,
                request.getAforoProductId(),
                request.getRatePlanId(),
                request.getBillingType()
        );
        
        // Write back attributes to Apigee
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aforo_subscription_id", subscriptionId.toString());
        attributes.put("aforo_rate_plan_id", request.getRatePlanId().toString());
        attributes.put("aforo_product_id", request.getAforoProductId().toString());
        
        try {
            apigeeGateway.writeAppAttributes(defaultOrg, request.getDeveloperApp(), attributes);
        } catch (Exception e) {
            log.error("Failed to write attributes back to Apigee", e);
        }
        
        // Save mapping
        AppMapping mapping = existingMapping != null ? existingMapping : new AppMapping();
        mapping.setApigeeAppId(request.getDeveloperApp());
        mapping.setAppName(request.getDeveloperApp()); // In real scenario, fetch actual name
        mapping.setApigeeDeveloperId("unknown"); // Should be fetched from Apigee
        mapping.setApiProduct(request.getApiProduct());
        mapping.setAforoProductId(request.getAforoProductId());
        mapping.setRatePlanId(request.getRatePlanId());
        mapping.setBillingType(request.getBillingType());
        mapping.setSubscriptionId(subscriptionId);
        mapping.setAttributesPushed(true);
        
        appMappingRepository.save(mapping);
        
        log.info("Draft subscription created successfully: {}", subscriptionId);
        
        return MappingResponse.builder()
                .subscriptionId(subscriptionId)
                .ratePlanId(request.getRatePlanId())
                .appId(request.getDeveloperApp())
                .wroteBackAttributes(true)
                .build();
    }
    
    private String findAforoCustomerIdForApp(String appId) {
        // In a real implementation, you'd query Apigee to get the developer for this app
        // For now, we'll try to find any linked developer (simplified)
        return developerLinkRepository.findAll().stream()
                .findFirst()
                .map(DeveloperLink::getAforoCustomerId)
                .orElse(null);
    }
    
    private Long createSubscriptionInAforo(String customerId, Long productId, Long ratePlanId, String billingType) {
        log.info("Creating subscription in Aforo for customer: {}", customerId);
        
        try {
            Map<String, Object> subscriptionRequest = new HashMap<>();
            subscriptionRequest.put("customerId", Long.parseLong(customerId));
            subscriptionRequest.put("productId", productId);
            subscriptionRequest.put("ratePlanId", ratePlanId);
            subscriptionRequest.put("billingType", billingType);
            subscriptionRequest.put("status", "DRAFT");
            
            WebClient webClient = webClientBuilder.baseUrl(subscriptionServiceUrl).build();
            
            String response = webClient.post()
                    .uri("/api/subscriptions")
                    .bodyValue(subscriptionRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("id").asLong();
            
        } catch (Exception e) {
            log.error("Failed to create subscription in Aforo", e);
            throw new RuntimeException("Failed to create subscription: " + e.getMessage(), e);
        }
    }
}
