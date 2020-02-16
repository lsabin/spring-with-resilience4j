package com.example.demo.api;

import com.example.demo.service.TheService;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

import static com.example.demo.service.TheService.BACKEND;

@RestController
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