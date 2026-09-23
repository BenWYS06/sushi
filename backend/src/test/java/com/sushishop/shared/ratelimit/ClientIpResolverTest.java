package com.sushishop.shared.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,::1");

    @Test
    void usesTheForwardedIpWhenTheCallerIsTrustedVite() {
        var request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.20");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void usesTheLastForwardedIpSoAnEarlierSpoofedValueIsIgnored() {
        var request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.20");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void ignoresForwardedIpFromAnUntrustedDirectCaller() {
        var request = requestFrom("198.51.100.20");
        request.addHeader("X-Forwarded-For", "203.0.113.99");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    private MockHttpServletRequest requestFrom(String remoteIp) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteIp);
        return request;
    }
}
