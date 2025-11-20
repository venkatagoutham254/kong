package com.aforo.apigee.service;

import com.aforo.apigee.dto.request.UsageWebhookEvent;

public interface UsageService {
    void ingestUsage(UsageWebhookEvent event);
}
