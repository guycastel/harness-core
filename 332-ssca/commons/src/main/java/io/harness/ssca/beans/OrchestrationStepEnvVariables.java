/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.SSCA)
public class OrchestrationStepEnvVariables {
  String sbomGenerationTool;
  String sbomGenerationFormat;
  String sbomSource;
  String sbomSourceType;
  String repoUrl;
  String repoPath;
  String repoVariant;
  String repoVariantType;
  String repoClonedCodebasePath;
  String sbomMode;
  String sbomDestination;
  String stepExecutionId;
  String stepIdentifier;
  String sscaCoreUrl;
  boolean sscaManagerEnabled;
  String sbomDrift;
  String sbomDriftVariant;
  String sbomDriftVariantType;
  boolean base64SecretAttestation;
  boolean airgapEnabled;
}
