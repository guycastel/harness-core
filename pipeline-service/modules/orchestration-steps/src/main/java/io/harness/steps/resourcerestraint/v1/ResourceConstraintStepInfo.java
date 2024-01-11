/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.v1;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.StepSpecTypeConstantsV1;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
@JsonTypeName(StepSpecTypeConstantsV1.RESOURCE_CONSTRAINT)
@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = HarnessModuleComponent.CDS_COMMON_STEPS)
public class ResourceConstraintStepInfo implements Visitable, PMSStepInfo {
  @JsonProperty(YamlNode.UUID_FIELD_NAME) String uuid;
  String identifier;
  @NotNull String name;
  @NotNull ParameterField<String> resourceUnit;
  @NotNull AcquireMode acquireMode;
  @NotNull int permits;
  @NotNull HoldingScope holdingScope;
  @Override
  public StepType getStepType() {
    return StepSpecTypeConstantsV1.RESOURCE_CONSTRAINT_STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return StepSpecTypeConstants.RESOURCE_RESTRAINT_FACILITATOR_TYPE;
  }
}
