package com.aforo.apigee.service;

import com.aforo.apigee.dto.request.CreateMappingRequest;
import com.aforo.apigee.dto.response.MappingResponse;

public interface MappingService {
    MappingResponse createDraftSubscription(CreateMappingRequest request);
}
