package com.sagares.saga_res_api.common.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(
                "Invalid email or password"
        );
    }
}