/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.approval.step.harness.v1;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Builder(builderMethodName = "infoBuilder")
@Value
@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_COMMON_STEPS})
@RecasterAlias("io.harness.steps.approval.step.harness.v1.HarnessApprovalStepParameters")
public class HarnessApprovalStepParameters implements SpecParameters {
  ParameterField<String> message;
  ParameterField<String> callback_id;
  ParameterField<Boolean> exclude_history;
  AutoApprovalParams auto_approval;
  Approvers approvers;
  List<ApproverInputInfo> inputs;
  ParameterField<Boolean> reject_previous;

  @Override
  public String getVersion() {
    return HarnessYamlVersion.V1;
  }
}
