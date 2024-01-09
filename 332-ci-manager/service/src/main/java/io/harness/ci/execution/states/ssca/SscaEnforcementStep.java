/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.states.ssca;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.outcome.StepArtifacts;
import io.harness.beans.steps.outcome.StepArtifacts.StepArtifactsBuilder;
import io.harness.ci.execution.states.AbstractStepExecutable;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadata;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadataType;
import io.harness.delegate.task.stepstatus.artifact.SscaArtifactMetadata;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.spec.server.ssca.v1.model.EnforcementSummaryResponse;
import io.harness.ssca.beans.SscaConstants;
import io.harness.ssca.client.SSCAServiceUtils;
import io.harness.ssca.client.beans.enforcement.SscaEnforcementSummary;
import io.harness.ssca.execution.orchestration.outcome.PublishedSbomArtifact;

import com.google.inject.Inject;
import java.util.Objects;

@OwnedBy(HarnessTeam.SSCA)
public class SscaEnforcementStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SscaConstants.SSCA_ENFORCEMENT_STEP_TYPE;

  @Inject SSCAServiceUtils sscaServiceUtils;

  @Override
  protected boolean shouldPublishArtifact(StepStatus stepStatus) {
    return true;
  }

  @Override
  protected boolean shouldPublishArtifactForVm(CommandExecutionStatus commandExecutionStatus) {
    return true;
  }

  @Override
  protected void modifyStepStatus(Ambiance ambiance, StepStatus stepStatus, String stepIdentifier) {
    String stepExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);

    if (sscaServiceUtils.isSSCAManagerEnabled()) {
      EnforcementSummaryResponse enforcementSummary =
          sscaServiceUtils.getEnforcementSummary(stepExecutionId, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

      stepStatus.setArtifactMetadata(
          ArtifactMetadata.builder()
              .type(ArtifactMetadataType.SSCA_ARTIFACT_METADATA)
              .spec(SscaArtifactMetadata.builder()
                        .id(enforcementSummary.getArtifact().getId())
                        .imageName(enforcementSummary.getArtifact().getName())
                        .registryUrl(enforcementSummary.getArtifact().getRegistryUrl())
                        .registryType(Objects.nonNull(enforcementSummary.getArtifact().getType())
                                ? enforcementSummary.getArtifact().getType().toString()
                                : null)
                        .stepExecutionId(stepExecutionId)
                        .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
                        .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
                        .exemptedComponentCount(enforcementSummary.getExemptedComponentCount())
                        .imageTag(enforcementSummary.getArtifact().getTag())
                        .build())
              .build());
    } else {
      SscaEnforcementSummary enforcementSummary = sscaServiceUtils.getEnforcementSummary(stepExecutionId);

      stepStatus.setArtifactMetadata(
          ArtifactMetadata.builder()
              .type(ArtifactMetadataType.SSCA_ARTIFACT_METADATA)
              .spec(SscaArtifactMetadata.builder()
                        .id(enforcementSummary.getArtifact().getId())
                        .imageName(enforcementSummary.getArtifact().getName())
                        .registryUrl(enforcementSummary.getArtifact().getUrl())
                        .registryType(enforcementSummary.getArtifact().getType())
                        .stepExecutionId(stepExecutionId)
                        .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
                        .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
                        .exemptedComponentCount(enforcementSummary.getExemptedComponentCount())
                        .imageTag(enforcementSummary.getArtifact().getTag())
                        .build())
              .build());
    }
  }

  @Override
  protected StepArtifacts handleArtifactForVm(
      ArtifactMetadata artifactMetadata, StepElementParameters stepParameters, Ambiance ambiance) {
    String stepExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (sscaServiceUtils.isSSCAManagerEnabled()) {
      EnforcementSummaryResponse enforcementSummary =
          sscaServiceUtils.getEnforcementSummary(stepExecutionId, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

      return StepArtifacts.builder()
          .publishedSbomArtifact(PublishedSbomArtifact.builder()
                                     .id(enforcementSummary.getArtifact().getId())
                                     .url(enforcementSummary.getArtifact().getRegistryUrl())
                                     .imageName(enforcementSummary.getArtifact().getName())
                                     .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
                                     .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
                                     .exemptedComponentCount(enforcementSummary.getExemptedComponentCount())
                                     .stepExecutionId(stepExecutionId)
                                     .tag(enforcementSummary.getArtifact().getTag())
                                     .build())
          .build();
    } else {
      SscaEnforcementSummary enforcementSummary = sscaServiceUtils.getEnforcementSummary(stepExecutionId);

      return StepArtifacts.builder()
          .publishedSbomArtifact(PublishedSbomArtifact.builder()
                                     .id(enforcementSummary.getArtifact().getId())
                                     .url(enforcementSummary.getArtifact().getUrl())
                                     .imageName(enforcementSummary.getArtifact().getName())
                                     .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
                                     .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
                                     .exemptedComponentCount(enforcementSummary.getExemptedComponentCount())
                                     .stepExecutionId(stepExecutionId)
                                     .tag(enforcementSummary.getArtifact().getTag())
                                     .build())
          .build();
    }
  }

  @Override
  protected StepArtifacts handleArtifact(ArtifactMetadata artifactMetadata, StepElementParameters stepParameters) {
    StepArtifactsBuilder stepArtifactsBuilder = StepArtifacts.builder();
    if (artifactMetadata == null) {
      return stepArtifactsBuilder.build();
    }
    if (artifactMetadata.getType() == ArtifactMetadataType.SSCA_ARTIFACT_METADATA) {
      SscaArtifactMetadata sscaArtifactMetadata = (SscaArtifactMetadata) artifactMetadata.getSpec();
      if (sscaArtifactMetadata != null) {
        stepArtifactsBuilder.publishedSbomArtifact(
            PublishedSbomArtifact.builder()
                .id(sscaArtifactMetadata.getId())
                .url(sscaArtifactMetadata.getRegistryUrl())
                .digest(sscaArtifactMetadata.getDigest())
                .imageName(sscaArtifactMetadata.getImageName())
                .stepExecutionId(sscaArtifactMetadata.getStepExecutionId())
                .allowListViolationCount(sscaArtifactMetadata.getAllowListViolationCount())
                .denyListViolationCount(sscaArtifactMetadata.getDenyListViolationCount())
                .exemptedComponentCount(sscaArtifactMetadata.getExemptedComponentCount())
                .tag(sscaArtifactMetadata.getImageTag())
                .build());
      }
    }
    return stepArtifactsBuilder.build();
  }
}
