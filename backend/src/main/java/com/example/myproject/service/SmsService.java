package com.example.myproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends SMS through Notify.lk (Sri Lankan SMS gateway).
 *
 * sms.enabled=false  -> nothing is sent, the SMS text is printed in the console (safe for testing)
 * sms.enabled=true   -> the SMS is sent through Notify.lk
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private static final String NOTIFY_URL = "https://app.notify.lk/api/v1/send";

    @Value("${sms.enabled:false}")
    private boolean enabled;

    @Value("${sms.notify.user-id:}")
    private String userId;

    @Value("${sms.notify.api-key:}")
    private String apiKey;

    @Value("${sms.notify.sender-id:NotifyDEMO}")
    private String senderId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public void send(String phone, String message) {

        String to = toInternationalFormat(phone);

        if (to == null) {
            log.warn("SMS not sent: '{}' is not a valid Sri Lankan phone number.", phone);
            return;
        }

        if (!enabled) {
            log.info("[SMS DISABLED - not really sent] To {}: {}", to, message);
            return;
        }

        if (userId.isBlank() || apiKey.isBlank()) {
            log.warn("SMS not sent: sms.notify.user-id / sms.notify.api-key are empty.");
            return;
        }

        try {

            String body =
                    "user_id=" + enc(userId) +
                    "&api_key=" + enc(apiKey) +
                    "&sender_id=" + enc(senderId) +
                    "&to=" + enc(to) +
                    "&message=" + enc(message);

            HttpRequest request = HttpRequest.newBuilder(URI.create(NOTIFY_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("SMS gateway response ({}): {}", response.statusCode(), response.body());

        } catch (Exception e) {
            log.error("Could not send SMS to {}: {}", to, e.getMessage());
        }
    }

    /** 0771234567 / 771234567 / +94771234567  ->  94771234567 */
    static String toInternationalFormat(String phone) {

        if (phone == null) {
            return null;
        }

        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() == 11 && digits.startsWith("94")) {
            return digits;
        }

        if (digits.length() == 10 && digits.startsWith("0")) {
            return "94" + digits.substring(1);
        }

        if (digits.length() == 9) {
            return "94" + digits;
        }

        return null;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}