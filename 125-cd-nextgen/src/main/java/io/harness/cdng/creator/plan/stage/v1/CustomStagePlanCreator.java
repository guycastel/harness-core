/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage.v1;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.beans.CustomStageSpecParams;
import io.harness.cdng.pipeline.steps.v1.CustomStageStep;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.plancreator.stages.v1.AbstractStagePlanCreator;
import io.harness.plancreator.stages.v1.StageParameterUtilsV1;
import io.harness.plancreator.steps.common.v1.StageElementParametersV1.StageElementParametersV1Builder;
import io.harness.plancreator.strategy.StrategyUtilsV1;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.when.utils.v1.RunInfoUtilsV1;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class CustomStagePlanCreator extends AbstractStagePlanCreator<CustomStageNodeV1> {
  @Override
  public CustomStageNodeV1 getFieldObject(YamlField field) {
    try {
      return YamlUtils.read(field.getNode().toString(), CustomStageNodeV1.class);
    } catch (IOException e) {
      throw new InvalidYamlException(
          "Unable to parse custom stage yaml. Please ensure that it is in correct format", e);
    }
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.STAGE, Collections.singleton(YAMLFieldNameConstants.CUSTOM_V1));
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, CustomStageNodeV1 field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    final Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));

    // Add dependency for execution
    YamlField stepsField = specField.getNode().getField(YAMLFieldNameConstants.STEPS);
    if (stepsField == null) {
      throw new InvalidRequestException("Steps section is required in Custom stage");
    }
    dependenciesNodeMap.put(specField.getNode().getUuid(), specField);

    // adding support for strategy
    Dependency strategyDependency = getDependencyForStrategy(dependenciesNodeMap, field, ctx);

    planCreationResponseMap.put(specField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                              .toBuilder()
                              .putDependencyMetadata(field.getUuid(), strategyDependency)
                              .putDependencyMetadata(specField.getNode().getUuid(), getDependencyForChildren(field))
                              .build())
            .build());

    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, CustomStageNodeV1 customStageNode, List<String> childrenNodeIds) {
    StageElementParametersV1Builder stageParameters =
        StageParameterUtilsV1.getCommonStageParametersBuilder(customStageNode);
    stageParameters.type(YAMLFieldNameConstants.CUSTOM_V1);
    stageParameters.spec(CustomStageSpecParams.builder().childNodeID(childrenNodeIds.get(0)).build());
    String name = customStageNode.getName();
    PlanNodeBuilder builder =
        PlanNode.builder()
            .uuid(StrategyUtilsV1.getSwappedPlanNodeId(ctx, customStageNode.getUuid()))
            .identifier(StrategyUtilsV1.getIdentifierWithExpression(ctx, customStageNode.getId()))
            .stepType(CustomStageStep.STEP_TYPE)
            .group(StepOutcomeGroup.STAGE.name())
            .name(StrategyUtilsV1.getIdentifierWithExpression(ctx, name))
            .skipUnresolvedExpressionsCheck(true)
            .whenCondition(RunInfoUtilsV1.getStageWhenCondition(customStageNode.getWhen()))
            .stepParameters(stageParameters.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .exports(customStageNode.getExports())
            .skipExpressionChain(false);

    // If strategy present then don't add advisers. Strategy node will take care of running the stage nodes.
    if (customStageNode.getStrategy() == null) {
      builder.adviserObtainments(getAdviserObtainments(ctx.getDependency()));
    }
    return builder.build();
  }
}
