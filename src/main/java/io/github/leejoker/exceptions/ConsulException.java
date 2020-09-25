package io.github.leejoker.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ConsulException extends RuntimeException {
    public ConsulException(String msg) {
        super(msg);
    }

    public ConsulException(Throwable throwable) {
        super(throwable);
    }

    public ConsulException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
