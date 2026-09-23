package com.sushishop.shared.exception.core;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

public class AiServiceUnavailableException extends SushiShopException {

    public AiServiceUnavailableException(String message) {
        super(SERVICE_UNAVAILABLE, message);
    }
}
