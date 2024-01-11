/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps.pluginstep;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.EnvironmentType;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.encryption.Scope;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.execution.StepsExecutionConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.plan.PluginCreationResponseList;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.TaskRequestsUtils;
import io.harness.steps.container.ContainerStepInitHelper;
import io.harness.steps.container.execution.ContainerExecutionConfig;
import io.harness.steps.container.execution.ContainerStepRbacHelper;
import io.harness.steps.container.utils.ConnectorUtils;
import io.harness.steps.container.utils.ContainerSpecUtils;
import io.harness.steps.executable.TaskExecutableWithRbac;
import io.harness.steps.matrix.ExpandedExecutionWrapperInfo;
import io.harness.steps.matrix.StrategyExpansionData;
import io.harness.steps.matrix.StrategyHelper;
import io.harness.steps.plugin.ContainerStepConstants;
import io.harness.steps.plugin.InitContainerV2StepInfo;
import io.harness.steps.plugin.StepInfo;
import io.harness.steps.plugin.infrastructure.ContainerK8sInfra;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.InitialiseTaskUtils;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ECS})
public class InitContainerV2Step implements TaskExecutableWithRbac<InitContainerV2StepInfo, K8sTaskExecutionResponse> {
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;

  @Inject ContainerStepInitHelper containerStepInitHelper;
  @Inject io.harness.plancreator.steps.pluginstep.ContainerStepV2PluginProvider containerStepV2PluginProvider;
  @Inject ContainerStepRbacHelper containerStepRbacHelper;
  @Inject ContainerExecutionConfig containerExecutionConfig;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject InitialiseTaskUtils initialiseTaskUtils;

  @Inject StrategyHelper strategyHelper;
  @Inject private ConnectorUtils connectorUtils;

  @Override
  public Class<InitContainerV2StepInfo> getStepParametersClass() {
    return InitContainerV2StepInfo.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, InitContainerV2StepInfo stepParameters) {
    containerStepRbacHelper.validateResources(stepParameters, ambiance);
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, InitContainerV2StepInfo stepParameters,
      ThrowingSupplier<K8sTaskExecutionResponse> responseDataSupplier) throws Exception {
    return initialiseTaskUtils.handleK8sTaskExecutionResponse(responseDataSupplier.get());
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, InitContainerV2StepInfo stepParameters, StepInputPackage inputPackage) {
    String logPrefix = initialiseTaskUtils.getLogPrefix(ambiance, "STEP");
    Map<String, StrategyExpansionData> strategyExpansionMap = new HashMap<>();
    List<ExecutionWrapperConfig> expandedExecutionElement = new ArrayList<>();

    for (ExecutionWrapperConfig config : stepParameters.getStepsExecutionConfig().getSteps()) {
      ExpandedExecutionWrapperInfo expandedExecutionWrapperInfo =
          strategyHelper.expandExecutionWrapperConfig(config, Optional.empty());
      expandedExecutionElement.addAll(expandedExecutionWrapperInfo.getExpandedExecutionConfigs());
      strategyExpansionMap.putAll(expandedExecutionWrapperInfo.getUuidToStrategyExpansionData());
    }

    stepParameters.setStepsExecutionConfig(StepsExecutionConfig.builder().steps(expandedExecutionElement).build());
    stepParameters.setStrategyExpansionMap(strategyExpansionMap);
    Map<StepInfo, PluginCreationResponseList> pluginsData =
        containerStepV2PluginProvider.getPluginsDataV2(stepParameters, ambiance);
    stepParameters.setPluginsData(pluginsData);

    CIInitializeTaskParams buildSetupTaskParams = containerStepInitHelper.getK8InitializeTaskParams(
        stepParameters, ambiance, logPrefix, stepParameters.getStepGroupIdentifier());

    String stageId = ambiance.getStageExecutionId();
    consumeExecutionConfig(ambiance);
    initialiseTaskUtils.constructStageDetails(
        ambiance, stepParameters.getIdentifier(), stepParameters.getName(), StepOutcomeGroup.STEP_GROUP.name());

    TaskData taskData = initialiseTaskUtils.getTaskData(buildSetupTaskParams);
    List<TaskSelector> taskSelectors = getTaskSelectors(ambiance, stepParameters);
    return TaskRequestsUtils.prepareTaskRequest(ambiance, taskData, referenceFalseKryoSerializer,
        TaskCategory.DELEGATE_TASK_V2, null, true, TaskType.valueOf(taskData.getTaskType()).getDisplayName(),
        taskSelectors, Scope.PROJECT, EnvironmentType.ALL, false, new ArrayList<>(), false, stageId);
  }

  @NotNull
  private ArrayList<TaskSelector> getTaskSelectors(Ambiance ambiance, InitContainerV2StepInfo stepParameters) {
    ContainerK8sInfra containerK8sInfra = (ContainerK8sInfra) stepParameters.getInfrastructure();
    String connectorName = containerK8sInfra.getSpec().getConnectorRef().getValue();
    ConnectorDetails k8sConnector =
        connectorUtils.getConnectorDetails(AmbianceUtils.getNgAccess(ambiance), connectorName);
    List<TaskSelector> connectorDelegateSelectors = ContainerSpecUtils.getConnectorDelegateSelectors(k8sConnector);
    return new ArrayList<>(connectorDelegateSelectors);
  }
  private void consumeExecutionConfig(Ambiance ambiance) {
    executionSweepingOutputService.consume(ambiance, ContainerStepConstants.CONTAINER_EXECUTION_CONFIG,
        containerExecutionConfig, StepCategory.STEP_GROUP.name());
  }
}
