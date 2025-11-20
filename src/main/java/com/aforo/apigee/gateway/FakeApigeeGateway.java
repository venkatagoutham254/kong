package com.aforo.apigee.gateway;

import com.aforo.apigee.dto.ApigeeCustomer;
import com.aforo.apigee.dto.response.ApiProductResponse;
import com.aforo.apigee.dto.response.DeveloperAppResponse;
import com.aforo.apigee.dto.response.DeveloperResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "aforo.apigee.fake", havingValue = "true", matchIfMissing = true)
public class FakeApigeeGateway implements ApigeeGateway {
    
    @Override
    public List<ApiProductResponse> listApiProducts(String org) {
        log.info("FakeApigeeGateway: Listing API products for org: {}", org);
        return Arrays.asList(
            ApiProductResponse.builder()
                .name("aadhaar-kyc-product")
                .displayName("Aadhaar KYC API Product")
                .quota("10000")
                .resources(Arrays.asList("/v1/kyc/**", "/v1/verify/**"))
                .build(),
            ApiProductResponse.builder()
                .name("pan-verification-product")
                .displayName("PAN Verification API Product")
                .quota("5000")
                .resources(Arrays.asList("/v1/pan/**"))
                .build(),
            ApiProductResponse.builder()
                .name("banking-apis-product")
                .displayName("Banking APIs Product")
                .quota("20000")
                .resources(Arrays.asList("/v1/accounts/**", "/v1/transactions/**"))
                .build()
        );
    }
    
    @Override
    public List<DeveloperResponse> listDevelopers(String org) {
        log.info("FakeApigeeGateway: Listing developers for org: {}", org);
        
        Map<String, String> attrs1 = new HashMap<>();
        attrs1.put("company", "ICICI Bank");
        attrs1.put("tier", "premium");
        
        Map<String, String> attrs2 = new HashMap<>();
        attrs2.put("company", "HDFC Bank");
        attrs2.put("tier", "standard");
        
        Map<String, String> attrs3 = new HashMap<>();
        attrs3.put("company", "Axis Bank");
        attrs3.put("tier", "premium");
        
        return Arrays.asList(
            DeveloperResponse.builder()
                .id("icici-001")
                .email("api-dev@icicibank.com")
                .attributes(attrs1)
                .build(),
            DeveloperResponse.builder()
                .id("hdfc-002")
                .email("integration@hdfcbank.com")
                .attributes(attrs2)
                .build(),
            DeveloperResponse.builder()
                .id("axis-003")
                .email("tech@axisbank.com")
                .attributes(attrs3)
                .build()
        );
    }
    
    @Override
    public List<DeveloperAppResponse> listDeveloperApps(String org, String developerId) {
        log.info("FakeApigeeGateway: Listing apps for developer: {} in org: {}", developerId, org);
        
        Map<String, String> appAttrs1 = new HashMap<>();
        appAttrs1.put("environment", "production");
        appAttrs1.put("version", "2.1.0");
        
        Map<String, String> appAttrs2 = new HashMap<>();
        appAttrs2.put("environment", "staging");
        appAttrs2.put("version", "1.5.3");
        
        if ("icici-001".equals(developerId)) {
            return Arrays.asList(
                DeveloperAppResponse.builder()
                    .appId("icici-mobile-app")
                    .name("ICICI Mobile Banking App")
                    .products(Arrays.asList("aadhaar-kyc-product", "banking-apis-product"))
                    .attributes(appAttrs1)
                    .build(),
                DeveloperAppResponse.builder()
                    .appId("icici-web-portal")
                    .name("ICICI Web Portal")
                    .products(Arrays.asList("pan-verification-product"))
                    .attributes(appAttrs2)
                    .build()
            );
        } else if ("hdfc-002".equals(developerId)) {
            return Arrays.asList(
                DeveloperAppResponse.builder()
                    .appId("hdfc-retail-app")
                    .name("HDFC Retail App")
                    .products(Arrays.asList("aadhaar-kyc-product"))
                    .attributes(appAttrs1)
                    .build()
            );
        } else if ("axis-003".equals(developerId)) {
            return Arrays.asList(
                DeveloperAppResponse.builder()
                    .appId("axis-corporate-app")
                    .name("Axis Corporate Banking")
                    .products(Arrays.asList("banking-apis-product", "pan-verification-product"))
                    .attributes(appAttrs1)
                    .build()
            );
        }
        
        return Arrays.asList();
    }
    
    @Override
    public void writeAppAttributes(String org, String appId, Map<String, String> attributes) {
        log.info("FakeApigeeGateway: Writing attributes to app: {} in org: {}", appId, org);
        log.debug("Attributes: {}", attributes);
        // In fake mode, just log - no actual API call
    }
    
    @Override
    public boolean testConnection(String org, String serviceAccountJson) {
        log.info("FakeApigeeGateway: Testing connection for org: {}", org);
        return true;
    }
    
    @Override
    public List<ApigeeCustomer> fetchDevelopers(String org, String env) {
        log.info("FakeApigeeGateway: Fetching developers for org: {}, env: {}", org, env);
        
        return Arrays.asList(
            ApigeeCustomer.builder()
                .developerId("dev-001")
                .email("developer1@example.com")
                .firstName("John")
                .lastName("Developer")
                .userName("john.dev")
                .organizationName("Example Corp")
                .status("active")
                .build(),
            ApigeeCustomer.builder()
                .developerId("dev-002")
                .email("developer2@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .userName("jane.smith")
                .organizationName("Tech Inc")
                .status("active")
                .build(),
            ApigeeCustomer.builder()
                .developerId("dev-003")
                .email("developer3@example.com")
                .firstName("Bob")
                .lastName("Johnson")
                .userName("bob.johnson")
                .organizationName("Innovation Labs")
                .status("active")
                .build()
        );
    }
}
