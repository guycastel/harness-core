/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.enforcement;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.SSCA)
public class EnforcementStepEnvVariables {
  String sbomSource;
  String sbomSourceType;
  String repoUrl;
  String repoPath;
  String repoVariant;
  String repoVariantType;
  String harnessPolicyFileId;
  String stepExecutionId;
  boolean sscaManagerEnabled;
  String policySetRef;
  boolean base64SecretAttestation;
  boolean airgapEnabled;
}
