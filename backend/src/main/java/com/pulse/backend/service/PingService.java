package com.pulse.backend.service;

import com.pulse.backend.model.PingResult;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class PingService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public PingResult checkHealth(String url) {
        long startTime = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long duration = System.currentTimeMillis() - startTime;
            int code = response.statusCode();

            String status = (code >= 200 && code < 400) ? "UP" : "DOWN";
            return new PingResult(code, duration, status);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new PingResult(0, duration, "DOWN");
        }
    }
}