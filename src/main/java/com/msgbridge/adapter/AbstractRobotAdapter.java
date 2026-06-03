package com.msgbridge.adapter;

import com.msgbridge.core.ChannelType;
import com.msgbridge.service.JsonService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

abstract class AbstractRobotAdapter implements MessageChannelAdapter {
    protected final WebClient webClient;
    protected final JsonService jsonService;

    protected AbstractRobotAdapter(WebClient.Builder webClientBuilder, JsonService jsonService) {
        this.webClient = webClientBuilder.build();
        this.jsonService = jsonService;
    }

    protected SendResult postJson(ChannelType platform, Long channelId, String url, Map<String, Object> body, int timeoutSeconds) {
        if (url == null || url.isBlank()) {
            return SendResult.failure(platform, channelId, "CONFIG_MISSING_WEBHOOK", "webhook_url is required", false);
        }
        String requestJson = jsonService.toJson(body);
        Instant start = Instant.now();
        try {
            String response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)))
                    .block();
            int cost = (int) Duration.between(start, Instant.now()).toMillis();
            return parsePlatformResponse(platform, channelId, requestJson, response, cost);
        } catch (WebClientResponseException e) {
            int cost = (int) Duration.between(start, Instant.now()).toMillis();
            boolean retryable = e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429;
            return new SendResult(
                    false,
                    platform,
                    channelId,
                    "HTTP_" + e.getStatusCode().value(),
                    e.getStatusText(),
                    requestJson,
                    e.getResponseBodyAsString(),
                    cost,
                    retryable);
        } catch (RuntimeException e) {
            int cost = (int) Duration.between(start, Instant.now()).toMillis();
            return new SendResult(false, platform, channelId, "NETWORK_ERROR", e.getMessage(), requestJson, null, cost, true);
        }
    }

    protected abstract SendResult parsePlatformResponse(
            ChannelType platform,
            Long channelId,
            String requestJson,
            String rawResponse,
            int costMs);
}
