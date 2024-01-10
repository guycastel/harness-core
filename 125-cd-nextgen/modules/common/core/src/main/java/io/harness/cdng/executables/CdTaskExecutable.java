/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.execution.service.StageExecutionInstanceInfoService;
import io.harness.delegate.beans.CDDelegateTaskNotifyResponseData;
import io.harness.delegate.cdng.execution.StepExecutionInstanceInfo;
import io.harness.opaclient.OpaServiceClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.steps.executable.TaskExecutableWithCapabilities;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.telemetry.helpers.DeploymentsInstrumentationHelper;
import io.harness.telemetry.helpers.StepExecutionTelemetryEventDTO;
import io.harness.utils.PolicyEvalUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
// Task Executable with RBAC, Rollback and postTaskValidation
public abstract class CdTaskExecutable<R extends ResponseData> extends TaskExecutableWithCapabilities<R> {
  @Inject OpaServiceClient opaServiceClient;
  @Inject StageExecutionInstanceInfoService stageExecutionInstanceInfoService;

  @Inject private DeploymentsInstrumentationHelper deploymentsInstrumentationHelper;

  // evaluating policies added in advanced section of the steps and updating status and failure info in the step
  // response
  public StepResponse postTaskValidate(
      Ambiance ambiance, StepBaseParameters stepParameters, StepResponse stepResponse) {
    if (Status.SUCCEEDED.equals(stepResponse.getStatus())) {
      return PolicyEvalUtils.evalPolicies(ambiance, stepParameters, stepResponse, opaServiceClient);
    }
    return stepResponse;
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(
      Ambiance ambiance, StepBaseParameters stepParameters, ThrowingSupplier<R> responseDataSupplier) throws Exception {
    saveNodeInfo(ambiance, responseDataSupplier);
    handleTelemetryProperties(ambiance, stepParameters);
    return handleTaskResultWithSecurityContextAndNodeInfo(ambiance, stepParameters, responseDataSupplier);
  }

  public abstract StepResponse handleTaskResultWithSecurityContextAndNodeInfo(
      Ambiance ambiance, StepBaseParameters stepParameters, ThrowingSupplier<R> responseDataSupplier) throws Exception;

  private void handleTelemetryProperties(Ambiance ambiance, StepBaseParameters stepParameters) {
    StepExecutionTelemetryEventDTO telemetryEventDTO = getStepExecutionTelemetryEventDTO(ambiance, stepParameters);
    if (telemetryEventDTO != null) {
      deploymentsInstrumentationHelper.publishStepEvent(ambiance, telemetryEventDTO);
    }
  }

  protected StepExecutionTelemetryEventDTO getStepExecutionTelemetryEventDTO(
      Ambiance ambiance, StepBaseParameters stepParameters) {
    return null;
  }

  private void saveNodeInfo(Ambiance ambiance, ThrowingSupplier<R> responseSupplier) {
    if (responseSupplier != null) {
      try {
        ResponseData responseData = responseSupplier.get();

        if (responseData instanceof CDDelegateTaskNotifyResponseData) {
          StepExecutionInstanceInfo stepExecutionInstanceInfo =
              ((CDDelegateTaskNotifyResponseData) responseData).getStepExecutionInstanceInfo();
          if (stepExecutionInstanceInfo != null) {
            stageExecutionInstanceInfoService.append(AmbianceUtils.getAccountId(ambiance),
                AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance),
                AmbianceUtils.getPipelineExecutionIdentifier(ambiance),
                AmbianceUtils.getStageExecutionIdForExecutionMode(ambiance), stepExecutionInstanceInfo,
                AmbianceUtils.getStepPath(ambiance));
          }
        }
      } catch (Exception ex) {
        log.error("Failed to save node info", ex);
      }
    }
  }
}
