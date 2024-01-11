/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.k8s.K8sDryRunManifestRequest.K8sDryRunManifestRequestBuilder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.executables.CdTaskChainExecutable;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.CustomFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sDryRunManifestRequest;
import io.harness.delegate.task.k8s.K8sDryRunManifestResponse;
import io.harness.exception.NestedExceptionUtils;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.k8s.exception.KubernetesExceptionExplanation;
import io.harness.k8s.exception.KubernetesExceptionHints;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.telemetry.helpers.StepExecutionTelemetryEventDTO;
import io.harness.telemetry.helpers.StepsInstrumentationHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class K8sDryRunManifestStep extends CdTaskChainExecutable implements K8sStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.K8S_DRY_RUN_MANIFEST.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  private final String K8S_DRY_RUN_MANIFEST = "K8s Dry Run";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject protected KryoSerializer kryoSerializer;
  @Inject protected StepHelper stepHelper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private StepsInstrumentationHelper stepsInstrumentationHelper;

  @Override
  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      StepBaseParameters stepParameters, List<String> manifestOverrideContents,
      K8sExecutionPassThroughData executionPassThroughData, boolean shouldOpenFetchFilesLogStream,
      UnitProgressData unitProgressData) {
    final InfrastructureOutcome infrastructure = executionPassThroughData.getInfrastructure();
    final String releaseName = cdStepHelper.getReleaseName(ambiance, infrastructure);
    final String accountId = AmbianceUtils.getAccountId(ambiance);
    List<String> manifestFilesContents =
        k8sStepHelper.renderValues(k8sManifestOutcome, ambiance, manifestOverrideContents);
    boolean isOpenshiftTemplate = ManifestType.OpenshiftTemplate.equals(k8sManifestOutcome.getType());

    K8sDryRunManifestStepParameters k8sDryRunManifestStepParameters =
        (K8sDryRunManifestStepParameters) stepParameters.getSpec();
    boolean isCanaryWorkflow = false;
    String canaryStepFqn = k8sDryRunManifestStepParameters.getCanaryStepFqn();
    if (canaryStepFqn != null) {
      OptionalSweepingOutput optionalCanaryOutcome = executionSweepingOutputService.resolveOptional(ambiance,
          RefObjectUtils.getSweepingOutputRefObject(
              canaryStepFqn + "." + OutcomeExpressionConstants.K8S_CANARY_OUTCOME));
      isCanaryWorkflow = optionalCanaryOutcome.isFound();
    }

    K8sDryRunManifestRequestBuilder k8sDryRunManifestRequestbuilder =
        K8sDryRunManifestRequest.builder()
            .releaseName(releaseName)
            .commandName(K8S_DRY_RUN_MANIFEST)
            .inCanaryWorkflow(isCanaryWorkflow)
            .localOverrideFeatureFlag(false)
            .timeoutIntervalInMin(CDStepHelper.getTimeoutInMin(stepParameters))
            .valuesYamlList(!isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .openshiftParamList(isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .kustomizePatchesList(k8sStepHelper.renderPatches(k8sManifestOutcome, ambiance, manifestOverrideContents))
            .k8sInfraDelegateConfig(cdStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(
                k8sStepHelper.getManifestDelegateConfigWrapper(executionPassThroughData.getZippedManifestId(),
                    k8sManifestOutcome, ambiance, executionPassThroughData.getManifestFiles()))
            .accountId(accountId)
            .shouldOpenFetchFilesLogStream(shouldOpenFetchFilesLogStream)
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .useLatestKustomizeVersion(cdStepHelper.isUseLatestKustomizeVersion(accountId))
            .useNewKubectlVersion(cdStepHelper.isUseNewKubectlVersion(accountId))
            .useDeclarativeRollback(k8sStepHelper.isDeclarativeRollbackEnabled(k8sManifestOutcome))
            .disableFabric8(cdStepHelper.shouldDisableFabric8(accountId));

    k8sDryRunManifestRequestbuilder.serviceHooks(k8sStepHelper.getServiceHooks(ambiance));

    K8sDryRunManifestRequest k8sDryRunManifestRequest = k8sDryRunManifestRequestbuilder.build();
    k8sStepHelper.publishReleaseNameStepDetails(ambiance, releaseName);
    return k8sStepHelper.queueK8sTask(stepParameters, k8sDryRunManifestRequest, ambiance, executionPassThroughData,
        TaskType.K8S_DRY_RUN_MANIFEST_TASK_NG);
  }

  @Override
  public Class<StepBaseParameters> getStepParametersClass() {
    return StepBaseParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepBaseParameters stepParameters) {
    // Noop
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContextAndNodeInfo(Ambiance ambiance,
      StepBaseParameters stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    log.info("Calling executeNextLink");
    return k8sStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContextAndNodeInfo(Ambiance ambiance,
      StepBaseParameters stepParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof CustomFetchResponsePassThroughData) {
      return k8sStepHelper.handleCustomTaskFailure((CustomFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return cdStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof HelmValuesFetchResponsePassThroughData) {
      return k8sStepHelper.handleHelmValuesFetchFailure((HelmValuesFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof StepExceptionPassThroughData) {
      return cdStepHelper.handleStepExceptionFailure((StepExceptionPassThroughData) passThroughData);
    }

    K8sExecutionPassThroughData executionPassThroughData = (K8sExecutionPassThroughData) passThroughData;

    K8sDeployResponse k8sTaskExecutionResponse;
    try {
      k8sTaskExecutionResponse = (K8sDeployResponse) responseDataSupplier.get();
    } catch (Exception e) {
      log.error("Error while processing K8s Task response: {}", e.getMessage(), e);
      return k8sStepHelper.handleTaskException(ambiance, executionPassThroughData, e);
    }

    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper.getFailureResponseBuilder(k8sTaskExecutionResponse, stepResponseBuilder).build();
    }
    K8sDryRunManifestResponse k8sDryRunManifestResponse =
        (K8sDryRunManifestResponse) k8sTaskExecutionResponse.getK8sNGTaskResponse();
    String manifestDryRun = k8sDryRunManifestResponse.getManifestDryRunYaml();
    if (isEmpty(manifestDryRun)) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.TOO_LARGE_MANIFEST_YAML,
          KubernetesExceptionExplanation.MANIFEST_SIZE_LIMIT,
          new UnsupportedOperationException("Unable to store k8s manifest dry run file as variable"));
    }
    K8sDryRunManifestOutcome k8sDryRunManifestOutcome = K8sDryRunManifestOutcome.builder()
                                                            .manifestDryRun(manifestDryRun)
                                                            .manifest(executionPassThroughData.getK8sGitFetchInfo())
                                                            .build();
    executionSweepingOutputService.consume(
        ambiance, k8sDryRunManifestOutcome.OUTPUT_NAME, k8sDryRunManifestOutcome, StepOutcomeGroup.STEP.name());
    return stepResponseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(k8sDryRunManifestOutcome.OUTPUT_NAME)
                         .outcome(k8sDryRunManifestOutcome)
                         .build())
        .build();
  }

  @Override
  protected StepExecutionTelemetryEventDTO getStepExecutionTelemetryEventDTO(
      Ambiance ambiance, StepBaseParameters stepParameters, PassThroughData passThroughData) {
    return StepExecutionTelemetryEventDTO.builder().stepType(STEP_TYPE.getType()).build();
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepBaseParameters stepParameters, StepInputPackage inputPackage) {
    return k8sStepHelper.startChainLink(this, ambiance, stepParameters);
  }
}
