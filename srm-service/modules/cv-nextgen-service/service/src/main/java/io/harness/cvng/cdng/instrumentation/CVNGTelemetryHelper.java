/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.instrumentation;

import static io.harness.telemetry.helpers.InstrumentationConstants.ACCOUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.ORG;
import static io.harness.telemetry.helpers.InstrumentationConstants.PROJECT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.MonitoredServiceSpecType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.telemetry.helpers.InstrumentationHelper;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class CVNGTelemetryHelper extends InstrumentationHelper {
  public static final String CVNG_STEP_EXECUTION_EVENT = "cvng_step_execution_event";
  public static final String MONITORED_SERVICE_TYPE = "monitored_service_type";
  public static final String SENSITIVITY = "sensitivity";
  public static final String DURATION = "duration";
  public static final String FAIL_ON_NO_ANALYSIS = "fail_on_no_analysis";

  public static final String VERIFICATION_TYPE = "verification_type";

  public CompletableFuture<Void> publishCVNGStepExecutionEvent(
      CVNGStepTask cvngStepTask, MonitoredServiceSpecType monitoredServiceType, CVNGStepParameter stepParameters) {
    HashMap<String, Object> eventPropertiesMap = new HashMap<>();
    eventPropertiesMap.put(ACCOUNT, cvngStepTask.getAccountId());
    eventPropertiesMap.put(ORG, cvngStepTask.getOrgIdentifier());
    eventPropertiesMap.put(PROJECT, cvngStepTask.getProjectIdentifier());
    eventPropertiesMap.put(MONITORED_SERVICE_TYPE, monitoredServiceType.getName());
    if (stepParameters != null && stepParameters.getSpec() != null) {
      eventPropertiesMap.put(SENSITIVITY, stepParameters.getSpec().getSensitivity().fetchFinalValue());
      eventPropertiesMap.put(DURATION, stepParameters.getSpec().getDuration().fetchFinalValue());
      eventPropertiesMap.put(FAIL_ON_NO_ANALYSIS, stepParameters.getSpec().getFailOnNoAnalysis().fetchFinalValue());
      eventPropertiesMap.put(VERIFICATION_TYPE, stepParameters.getSpec().getType());
    }

    return sendEvent(CVNG_STEP_EXECUTION_EVENT, cvngStepTask.getAccountId(), eventPropertiesMap);
  }
}
