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
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.resourcerestraint.ResourceRestraintSpecParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;

import lombok.Builder;

@Builder
@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = HarnessModuleComponent.CDS_COMMON_STEPS)
public class ResourceConstraintSpecParameters implements SpecParameters {
  private String name;
  private ParameterField<String> resourceUnit;
  private AcquireMode acquireMode;
  private int permits;
  private HoldingScope holdingScope;
  public ResourceRestraintSpecParameters toResourceRestraintSpecParametersV0() {
    return ResourceRestraintSpecParameters.builder()
        .name(name)
        .resourceUnit(resourceUnit)
        .acquireMode(acquireMode)
        .holdingScope(holdingScope)
        .build();
  }

  @Override
  public String getVersion() {
    return HarnessYamlVersion.V1;
  }
}
