package com.aforo.apigee.service;

import com.aforo.apigee.dto.request.LinkDeveloperRequest;
import com.aforo.apigee.dto.request.SaveConnectionRequest;
import com.aforo.apigee.dto.response.*;

import java.util.List;

public interface InventoryService {
    ConnectionResponse saveAndTestConnection(SaveConnectionRequest request);
    List<ApiProductResponse> getApiProducts(String org);
    List<DeveloperResponse> getDevelopers(String org);
    List<DeveloperAppResponse> getDeveloperApps(String developerId, String org);
    void linkDeveloper(String developerId, LinkDeveloperRequest request);
    CustomerSyncResponse syncCustomers();
}
