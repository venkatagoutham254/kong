package com.aforo.apigee.service;

import com.aforo.apigee.dto.request.AuthorizeRequest;
import com.aforo.apigee.dto.response.AuthorizeDecision;

public interface AuthorizeService {
    AuthorizeDecision authorize(AuthorizeRequest request);
}
