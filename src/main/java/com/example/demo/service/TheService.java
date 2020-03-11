package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Random;

@Service
@Slf4j
public class TheService {

    public static final String BACKEND = "backend";

    @CircuitBreaker(name = BACKEND)
    @Bulkhead(name = BACKEND)
    @Retry(name = BACKEND)
    public String failure() {
        log.info("Calling failing server...");

        int random = new Random().nextInt(15);

        if (random < 5) {
            log.info("Success!!!");
            return "Successful call to server";
        } else {
            log.info("Exception thrown");
            throw new IllegalStateException();
        }


    }

    @CircuitBreaker(name = BACKEND)
    @Bulkhead(name = BACKEND)
    public String ignoreException() {
        throw new BusinessException("This exception is ignored by the CircuitBreaker of backend");
    }

    @CircuitBreaker(name = BACKEND)
    @Bulkhead(name = BACKEND)
    @Retry(name = BACKEND)
    public String success() {
        log.info("Success!!!");
        return "Hello World from backend";
    }

    @CircuitBreaker(name = BACKEND)
    @Bulkhead(name = BACKEND)
    public String successException() {
        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "This is a remote client exception");
    }

    @CircuitBreaker(name = BACKEND, fallbackMethod = "fallback")
    public String failureWithFallback() {
        return failure();
    }


    @TimeLimiter(name = BACKEND)
    public String slow() {
        timeout();
        return "Hello World from backend";
    }


    private String fallback(HttpServerErrorException ex) {
        return "Recovered HttpServerErrorException: " + ex.getMessage();
    }

    private String fallback(Exception ex) {
        return "Recovered: " + ex.toString();
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
