package com.example.demo.api;

import com.example.demo.service.TheService;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.example.demo.service.TheService.BACKEND;
import static java.time.temporal.ChronoUnit.SECONDS;

@RestController
@Slf4j
public class TheController {

    private final TheService service;

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final ThreadPoolBulkhead threadPoolBulkhead;
    private final Retry retry;

    @Autowired
    public TheController(TheService service,
                        CircuitBreakerRegistry circuitBreakerRegistry, BulkheadRegistry bulkheadRegistry,
                         ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry, RetryRegistry retryRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(BACKEND);
        this.bulkhead = bulkheadRegistry.bulkhead(BACKEND);
        this.threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(BACKEND);
        this.retry = retryRegistry.retry(BACKEND);
        this.service = service;

        log.info("Config retry: {}", retryRegistry.getConfiguration(BACKEND));
    }

    @GetMapping("failure")
    public String failure() {
        return service.failure();
    }

    @GetMapping("success")
    public String success() {
        return service.success();
    }

    @GetMapping("successException")
    public String successException() {
        return service.successException();
    }


    @GetMapping("failure-decorator")
    public String failureWithDecorator() {
        return execute(service::failure);
    }

    @GetMapping("success-decorator")
    public String successWithDecorator() {
        return execute(service::success);
    }

    @GetMapping("successException-decorator")
    public String successExceptionWithDecorator() {
        return execute(service::successException);
    }

    @GetMapping("ignore")
    public String ignore() {
        return Decorators.ofSupplier(service::ignoreException)
                .withCircuitBreaker(circuitBreaker)
                .withBulkhead(bulkhead).get();
    }


    @GetMapping("fallback")
    public String failureWithFallback() {
        return service.failureWithFallback();
    }

    @GetMapping("timeout")
    public String withTimeout() {
        return service.slow();
    }

    @GetMapping("retry-configured")
    public String withRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .retryExceptions(NullPointerException.class, IllegalStateException.class)
                .waitDuration(Duration.of(3L, SECONDS))
                .intervalFunction(IntervalFunction.ofExponentialBackoff())
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        Retry retry = retryRegistry.retry("theRetry");

        Supplier<String> supplier = Retry.decorateSupplier(retry, () -> service.failure());

        return supplier.get();
    }

    private <T> T execute(Supplier<T> supplier){
        return Decorators.ofSupplier(supplier)
                .withCircuitBreaker(circuitBreaker)
                .withBulkhead(bulkhead)
                .withRetry(retry)
                .get();
    }

    private String timeout(){
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

}