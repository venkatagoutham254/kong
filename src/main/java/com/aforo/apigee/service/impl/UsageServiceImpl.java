package com.aforo.apigee.service.impl;

import com.aforo.apigee.dto.request.UsageWebhookEvent;
import com.aforo.apigee.model.UsageEvent;
import com.aforo.apigee.repository.UsageEventRepository;
import com.aforo.apigee.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageServiceImpl implements UsageService {
    
    private final UsageEventRepository usageEventRepository;
    
    @Override
    @Transactional
    public void ingestUsage(UsageWebhookEvent event) {
        log.info("Ingesting usage event for app: {} and product: {}", 
                 event.getDeveloperApp(), event.getApiProduct());
        
        // Convert event to raw JSON
        Map<String, Object> rawJson = new HashMap<>();
        rawJson.put("ts", event.getTs().toString());
        rawJson.put("apiproxy", event.getApiproxy());
        rawJson.put("developerApp", event.getDeveloperApp());
        rawJson.put("apiProduct", event.getApiProduct());
        rawJson.put("method", event.getMethod());
        rawJson.put("path", event.getPath());
        rawJson.put("status", event.getStatus());
        rawJson.put("latencyMs", event.getLatencyMs());
        rawJson.put("bytesOut", event.getBytesOut());
        if (event.getExtras() != null) {
            rawJson.put("extras", event.getExtras());
        }
        
        // Save to database
        UsageEvent usageEvent = UsageEvent.builder()
                .ts(event.getTs())
                .apiproxy(event.getApiproxy())
                .developerApp(event.getDeveloperApp())
                .apiProduct(event.getApiProduct())
                .method(event.getMethod())
                .path(event.getPath())
                .status(event.getStatus())
                .latencyMs(event.getLatencyMs())
                .bytesOut(event.getBytesOut())
                .rawJson(rawJson)
                .build();
        
        usageEventRepository.save(usageEvent);
        
        log.info("Usage event saved successfully");
        
        // Optionally forward to metering-service here
        // forwardToMeteringService(usageEvent);
    }
}
