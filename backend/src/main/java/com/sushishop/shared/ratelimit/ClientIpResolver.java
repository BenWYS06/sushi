package com.sushishop.shared.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Uses X-Forwarded-For only when the immediate caller is a proxy we trust.
 * Direct callers cannot choose their own rate-limit IP by sending that header.
 */
@Component
public class ClientIpResolver {

    private final Set<String> trustedProxyIps;

    public ClientIpResolver(
            @Value("${app.rate-limit.trusted-proxies:127.0.0.1,::1}") String trustedProxies) {
        this.trustedProxyIps = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteIp = request.getRemoteAddr();

        if (!trustedProxyIps.contains(remoteIp)) {
            return remoteIp;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteIp;
        }

        // Vite appends the original caller IP. Take the last non-empty value.
        String[] addresses = forwardedFor.split(",");
        for (int index = addresses.length - 1; index >= 0; index--) {
            String address = addresses[index].trim();
            if (!address.isEmpty()) {
                return address;
            }
        }

        return remoteIp;
    }
}
