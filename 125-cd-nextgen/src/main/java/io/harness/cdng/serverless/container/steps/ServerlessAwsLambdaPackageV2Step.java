/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless.container.steps;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.callback.DelegateCallbackToken;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.plugininfoproviders.ServerlessV2PluginInfoProviderHelper;
import io.harness.cdng.serverless.ServerlessStepCommonHelper;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plugin.AbstractContainerStepV2;
import io.harness.pms.sdk.core.plugin.ContainerUnitStepUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.tasks.ResponseData;
import io.harness.telemetry.helpers.StepExecutionTelemetryEventDTO;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_SERVERLESS})
@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class ServerlessAwsLambdaPackageV2Step extends AbstractContainerStepV2<StepElementParameters> {
  @Inject Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Inject ServerlessStepCommonHelper serverlessStepCommonHelper;

  @Inject ServerlessV2PluginInfoProviderHelper serverlessV2PluginInfoProviderHelper;

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Inject private InstanceInfoService instanceInfoService;

  public static final StepType STEP_TYPE =
      StepType.newBuilder()
          .setType(ExecutionNodeType.SERVERLESS_AWS_LAMBDA_PACKAGE_V2.getYamlType())
          .setStepCategory(StepCategory.STEP)
          .build();

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public long getTimeout(Ambiance ambiance, StepElementParameters stepElementParameters) {
    return Timeout.fromString((String) stepElementParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
  }

  @Override
  public UnitStep getSerialisedStep(Ambiance ambiance, StepElementParameters stepElementParameters, String accountId,
      String logKey, long timeout, String parkedTaskId) {
    ServerlessAwsLambdaPackageV2StepParameters serverlessAwsLambdaPackageV2StepParameters =
        (ServerlessAwsLambdaPackageV2StepParameters) stepElementParameters.getSpec();

    // Check if image exists
    serverlessStepCommonHelper.verifyPluginImageIsProvider(serverlessAwsLambdaPackageV2StepParameters.getImage());

    Map<String, String> envVarMap = serverlessV2PluginInfoProviderHelper.getEnvironmentVariables(
        ambiance, serverlessAwsLambdaPackageV2StepParameters);
    serverlessStepCommonHelper.putValuesYamlEnvVars(ambiance, serverlessAwsLambdaPackageV2StepParameters, envVarMap);

    serverlessV2PluginInfoProviderHelper.removeAllEnvVarsWithSecretRef(envVarMap);
    serverlessV2PluginInfoProviderHelper.validateEnvVariables(envVarMap);

    return getUnitStep(ambiance, stepElementParameters, accountId, logKey, parkedTaskId,
        serverlessAwsLambdaPackageV2StepParameters, envVarMap);
  }

  public UnitStep getUnitStep(Ambiance ambiance, StepElementParameters stepElementParameters, String accountId,
      String logKey, String parkedTaskId,
      ServerlessAwsLambdaPackageV2StepParameters serverlessAwsLambdaPackageV2StepParameters, Map envVarMap) {
    return ContainerUnitStepUtils.serializeStepWithStepParameters(
        getPort(ambiance, stepElementParameters.getIdentifier()), parkedTaskId, logKey,
        stepElementParameters.getIdentifier(), getTimeout(ambiance, stepElementParameters), accountId,
        stepElementParameters.getName(), delegateCallbackTokenSupplier, ambiance, envVarMap,
        serverlessAwsLambdaPackageV2StepParameters.getImage().getValue(), Collections.EMPTY_LIST);
  }

  @Override
  public StepResponse.StepOutcome getAnyOutComeForStep(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return null;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // we need to check if rbac check is req or not.
  }

  @Override
  public StepExecutionTelemetryEventDTO getStepExecutionTelemetryEventDTO(
      Ambiance ambiance, StepElementParameters stepElementParameters) {
    return StepExecutionTelemetryEventDTO.builder().stepType(STEP_TYPE.getType()).build();
  }
  @Override
  public ParameterField<List<TaskSelectorYaml>> getStepDelegateSelectors(SpecParameters stepElementParameters) {
    ServerlessAwsLambdaPackageV2StepParameters packageV2StepParameters =
        (ServerlessAwsLambdaPackageV2StepParameters) stepElementParameters;
    return packageV2StepParameters.getDelegateSelectors();
  }
}
