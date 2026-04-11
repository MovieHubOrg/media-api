package com.media.api.component;

import feign.RetryableException;
import feign.Retryer;

public class CustomRetryer implements Retryer {
    private final long maxTotalTime;
    private final long period;
    private long startTime;

    public CustomRetryer(long maxTotalTime, long period) {
        this.maxTotalTime = maxTotalTime;
        this.period = period;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void continueOrPropagate(RetryableException e) {
        if (System.currentTimeMillis() - startTime >= maxTotalTime) {
            throw e;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public Retryer clone() {
        return new CustomRetryer(maxTotalTime, period);
    }
}
