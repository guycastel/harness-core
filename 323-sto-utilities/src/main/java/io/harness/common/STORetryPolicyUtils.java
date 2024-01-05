/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;

import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

@OwnedBy(HarnessTeam.STO)
@UtilityClass
@Slf4j
public class STORetryPolicyUtils {
  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_DELAY_MS = 1000;
  private static final long MAX_DELAY_MS = 5000;
  private static final long DELAY_FACTOR = 2;

  private static RetryPolicy<Object> createRetryPolicy() {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withBackoff(INITIAL_DELAY_MS, MAX_DELAY_MS, ChronoUnit.MILLIS, DELAY_FACTOR)
        .withMaxAttempts(MAX_ATTEMPTS);
  }

  public static RetryPolicy<Object> getSTORetryPolicy(String failedAttemptMessage, String failureMessage) {
    return createRetryPolicy()
        .onFailedAttempt(event -> log.warn(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> {
          log.error(failureMessage, event.getAttemptCount(), event.getFailure());
          throw new GeneralException(event.getFailure().getMessage(), event.getFailure());
        });
  }

  public static RetryPolicy<Object> getSTORetryPolicyForToken(String failedAttemptMessage, String failureMessage) {
    return createRetryPolicy()
        .onFailedAttempt(event -> log.warn(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
