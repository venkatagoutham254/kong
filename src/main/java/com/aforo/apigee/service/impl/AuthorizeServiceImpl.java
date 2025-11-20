package com.aforo.apigee.service.impl;

import com.aforo.apigee.dto.request.AuthorizeRequest;
import com.aforo.apigee.dto.response.AuthorizeDecision;
import com.aforo.apigee.model.AppMapping;
import com.aforo.apigee.repository.AppMappingRepository;
import com.aforo.apigee.service.AuthorizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizeServiceImpl implements AuthorizeService {
    
    private final AppMappingRepository appMappingRepository;
    
    @Override
    public AuthorizeDecision authorize(AuthorizeRequest request) {
        log.info("Authorizing request for app: {} and product: {}", 
                 request.getDeveloperApp(), request.getApiProduct());
        
        // Lookup mapping
        AppMapping mapping = appMappingRepository
                .findByApigeeAppIdAndApiProduct(request.getDeveloperApp(), request.getApiProduct())
                .orElse(null);
        
        if (mapping == null) {
            log.warn("No mapping found for app: {} and product: {}", 
                     request.getDeveloperApp(), request.getApiProduct());
            return AuthorizeDecision.builder()
                    .allow(false)
                    .reason("No mapping found for this app and product combination")
                    .build();
        }
        
        if (mapping.getSubscriptionId() == null) {
            log.warn("Mapping exists but no subscription ID for app: {}", request.getDeveloperApp());
            return AuthorizeDecision.builder()
                    .allow(false)
                    .reason("No subscription associated with this mapping")
                    .build();
        }
        
        // In a full implementation, you would call subscription-service to check if subscription is ACTIVE
        // For now, we'll assume if mapping exists with subscription ID, it's allowed
        
        log.info("Authorization granted for app: {}", request.getDeveloperApp());
        return AuthorizeDecision.builder()
                .allow(true)
                .reason("Authorized")
                .subscriptionId(mapping.getSubscriptionId())
                .ratePlanId(mapping.getRatePlanId())
                .build();
    }
}
