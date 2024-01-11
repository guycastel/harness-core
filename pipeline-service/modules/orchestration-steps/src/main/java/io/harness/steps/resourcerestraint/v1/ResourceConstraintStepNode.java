/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.v1;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.v1.PmsAbstractStepNodeV1;
import io.harness.steps.StepSpecTypeConstantsV1;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = HarnessModuleComponent.CDS_COMMON_STEPS)
@JsonTypeName(StepSpecTypeConstantsV1.RESOURCE_CONSTRAINT)
@Value
public class ResourceConstraintStepNode extends PmsAbstractStepNodeV1 {
  String type = StepSpecTypeConstantsV1.RESOURCE_CONSTRAINT;

  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ResourceConstraintStepInfo spec;

  @Override
  public SpecParameters getSpecParameters() {
    return ResourceConstraintSpecParameters.builder()
        .name(spec.getName())
        .resourceUnit(spec.getResourceUnit())
        .acquireMode(spec.getAcquireMode())
        .holdingScope(spec.getHoldingScope())
        .build();
  }
}
