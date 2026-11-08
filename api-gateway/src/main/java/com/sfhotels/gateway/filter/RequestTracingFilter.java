package com.sfhotels.gateway.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestTracingFilter implements GlobalFilter, Ordered {

    public static final String TRACE_HEADER = "X-Trace-Id";

    private final Tracer tracer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startedAt = System.currentTimeMillis();
        Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .ifPresent(context -> exchange.getResponse().getHeaders().set(TRACE_HEADER, context.traceId()));
        log.info("Gateway request {} {}", request.getMethod(), request.getPath());
        return chain.filter(exchange).doFinally(signal -> log.info("Gateway response {} {} status={} durationMs={}",
                request.getMethod(), request.getPath(), exchange.getResponse().getStatusCode(),
                System.currentTimeMillis() - startedAt));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
