package com.sushishop.shared.ratelimit;

import com.sushishop.shared.exception.core.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {
        String key = rateLimit.key();
        if ("ip".equals(key)) {
            key = getClientIp();
        }

        boolean allowed = rateLimitService.tryConsume(
                key,
                1,
                rateLimit.value(),
                rateLimit.duration()
        );

        if (!allowed) {
            throw new RateLimitExceededException();
        }
    }

    private String getClientIp() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        return clientIpResolver.resolve(
                ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest());
    }
}
