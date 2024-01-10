/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage.v1;

import static io.harness.cdng.creator.plan.CDPlanCreationConstants.ENVIRONMENTS;
import static io.harness.cdng.creator.plan.CDPlanCreationConstants.SERVICES;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.stage.StagePlanCreatorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.HarnessStruct;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorConstants;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.common.NGSectionStep;
import io.harness.steps.common.NGSectionStepParameters;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = HarnessModuleComponent.CDS_K8S)
public class CDStageSpecPlanCreator extends ChildrenPlanCreator<DeploymentStageConfigV1> {
  @Inject private StagePlanCreatorHelper stagePlanCreatorHelper;

  @Override
  public DeploymentStageConfigV1 getFieldObject(YamlField field) {
    try {
      return YamlUtils.read(field.getNode().toString(), DeploymentStageConfigV1.class);
    } catch (IOException e) {
      throw new InvalidYamlException(
          "Unable to parse DeploymentStageConfigV1 yaml. Please ensure that it is in correct format", e);
    }
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.SPEC, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, DeploymentStageConfigV1 field) {
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();

    if (field.getEnvironments() == null) {
      throw new InvalidYamlException("Environments must be defined in Deployment stage");
    } else if (field.getServices() == null) {
      throw new InvalidYamlException("Services must be defined in Deployment stage");
    } else if (EmptyPredicate.isEmpty(field.getSteps())) {
      throw new InvalidYamlException("At least one step must be defined in the Deployment stage");
    }

    YamlField currentField = ctx.getCurrentField();

    // Adding services dependency.
    Dependency servicesDep =
        Dependency.newBuilder()
            .setNodeMetadata(
                HarnessStruct
                    .newBuilder()
                    // We create the planNode for infra but not for env. So EnvPlanCreator will create the planNode for
                    // infra.
                    .putData(PlanCreatorConstants.NEXT_ID,
                        HarnessValue.newBuilder().setStringValue(field.getEnvironments().getUuid()).build())
                    .putData(ENVIRONMENTS,
                        HarnessValue.newBuilder()
                            .setStringValue(ctx.getCurrentField()
                                                .getNode()
                                                .getField(ENVIRONMENTS)
                                                .getNode()
                                                .getCurrJsonNode()
                                                .toString())
                            .build())
                    .build())
            .build();
    responseMap.put(field.getServices().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(Dependencies.newBuilder()
                              .putDependencies(field.getServices().getUuid(),
                                  ctx.getCurrentField().getNode().getField(SERVICES).getYamlPath())
                              .putDependencyMetadata(field.getServices().getUuid(), servicesDep)
                              .build())
            .build());

    final boolean isProjectScopedResourceConstraintQueue =
        stagePlanCreatorHelper.isProjectScopedResourceConstraintQueueByFFOrSetting(ctx);
    YamlField rcField =
        InfrastructurePmsPlanCreator.constructResourceConstraintYamlField(ctx.getCurrentField().getNode(), null, ctx,
            isProjectScopedResourceConstraintQueue, YAMLFieldNameConstants.STEP, YAMLFieldNameConstants.STEP);

    // Adding environments dependency.
    Dependency envDependency =
        Dependency.newBuilder()
            .setNodeMetadata(HarnessStruct.newBuilder()
                                 .putData(PlanCreatorConstants.NEXT_ID,
                                     HarnessValue.newBuilder().setStringValue(rcField.getUuid()).build())
                                 .build())
            .build();
    responseMap.put(field.getEnvironments().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(Dependencies.newBuilder()
                              .putDependencies(field.getEnvironments().getUuid(),
                                  ctx.getCurrentField().getNode().getField(ENVIRONMENTS).getYamlPath())
                              .putDependencyMetadata(field.getEnvironments().getUuid(), envDependency)
                              .build())
            .build());

    YamlField stepsField = currentField.getNode().getField(YAMLFieldNameConstants.STEPS);
    YamlUpdates yamlUpdates;
    // YamlUpdates to add RC yaml field in the original yamlField to create RC plan.
    try {
      yamlUpdates =
          YamlUpdates.newBuilder().putFqnToYaml(rcField.getYamlPath(), YamlUtils.writeYamlString(rcField)).build();
    } catch (IOException e) {
      throw new InvalidRequestException("");
    }

    // Adding RC dependency
    responseMap.put(rcField.getUuid(),
        PlanCreationResponse.builder()
            .dependencies(
                Dependencies.newBuilder()
                    .putDependencies(rcField.getUuid(), rcField.getYamlPath())
                    .putDependencyMetadata(rcField.getUuid(),
                        Dependency.newBuilder()
                            .setNodeMetadata(
                                HarnessStruct.newBuilder()
                                    .putData(PlanCreatorConstants.NEXT_ID,
                                        HarnessValue.newBuilder().setStringValue(stepsField.getUuid()).build())
                                    .build())
                            .build())
                    .build())
            .yamlUpdates(yamlUpdates)
            .build());

    // Adding steps dependency.
    responseMap.put(stepsField.getUuid(),
        PlanCreationResponse.builder()
            .dependencies(
                Dependencies.newBuilder().putDependencies(stepsField.getUuid(), stepsField.getYamlPath()).build())
            .build());

    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, DeploymentStageConfigV1 stageConfigV1, List<String> childrenNodeIds) {
    StepParameters stepParameters = NGSectionStepParameters.builder().childNodeId(childrenNodeIds.get(0)).build();
    return PlanNode.builder()
        .uuid(stageConfigV1.getUuid())
        .identifier(YAMLFieldNameConstants.SPEC)
        .stepType(NGSectionStep.STEP_TYPE)
        .name(YAMLFieldNameConstants.SPEC)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}
