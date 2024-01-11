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
import io.harness.filters.v1.GenericStepPMSFilterJsonCreatorV3;
import io.harness.steps.StepSpecTypeConstantsV1;

import java.util.Set;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_COMMON_STEPS})
public class ResourceConstraintStepFilterCreator extends GenericStepPMSFilterJsonCreatorV3 {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Set.of(StepSpecTypeConstantsV1.RESOURCE_CONSTRAINT);
  }
}
