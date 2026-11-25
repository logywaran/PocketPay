package com.logy.pocketpay.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TraceContext {

    private final Tracer tracer;

    public String getTraceId() {
        Span currentSpan = tracer.currentSpan();

        return currentSpan != null
                ? currentSpan.context().traceId()
                : null;
    }
}
