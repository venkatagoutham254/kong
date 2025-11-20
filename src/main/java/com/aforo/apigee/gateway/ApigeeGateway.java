package com.aforo.apigee.gateway;

import com.aforo.apigee.dto.ApigeeCustomer;
import com.aforo.apigee.dto.response.ApiProductResponse;
import com.aforo.apigee.dto.response.DeveloperAppResponse;
import com.aforo.apigee.dto.response.DeveloperResponse;

import java.util.List;
import java.util.Map;

public interface ApigeeGateway {
    
    List<ApiProductResponse> listApiProducts(String org);
    
    List<DeveloperResponse> listDevelopers(String org);
    
    List<DeveloperAppResponse> listDeveloperApps(String org, String developerId);
    
    void writeAppAttributes(String org, String appId, Map<String, String> attributes);
    
    boolean testConnection(String org, String serviceAccountJson);
    
    List<ApigeeCustomer> fetchDevelopers(String org, String env);
}
