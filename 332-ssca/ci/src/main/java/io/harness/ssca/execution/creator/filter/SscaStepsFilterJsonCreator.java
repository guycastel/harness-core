/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.execution.creator.filter;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidYamlException;
import io.harness.filters.GenericStepPMSFilterJsonCreatorV2;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.ssca.beans.SscaConstants;
import io.harness.ssca.beans.sbomDrift.SbomDriftBase;
import io.harness.ssca.beans.source.RepositorySbomSource;
import io.harness.ssca.beans.source.SbomSourceType;
import io.harness.ssca.beans.stepinfo.SscaEnforcementStepInfo;
import io.harness.ssca.beans.stepinfo.SscaOrchestrationStepInfo;
import io.harness.ssca.beans.stepnode.SscaEnforcementStepNode;
import io.harness.ssca.beans.stepnode.SscaOrchestrationStepNode;

import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;

@OwnedBy(HarnessTeam.SSCA)
public class SscaStepsFilterJsonCreator extends GenericStepPMSFilterJsonCreatorV2 {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(SscaConstants.SSCA_ORCHESTRATION_STEP, SscaConstants.SSCA_ENFORCEMENT,
        SscaConstants.SLSA_PROVENANCE, SscaConstants.SLSA_VERIFICATION);
  }

  @Override
  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, AbstractStepNode yamlField) {
    validateStep(yamlField);
    return super.handleNode(filterCreationContext, yamlField);
  }

  public void validateStep(AbstractStepNode yamlField) {
    String stepType = yamlField.getType();
    switch (stepType) {
      case SscaConstants.SSCA_ORCHESTRATION_STEP:
        validateOrchestrationStep((SscaOrchestrationStepNode) yamlField);
        break;
      case SscaConstants.SSCA_ENFORCEMENT:
        validateEnforcementStep((SscaEnforcementStepNode) yamlField);
        break;
      default:
        break;
    }
  }

  public void validateOrchestrationStep(SscaOrchestrationStepNode orchestrationStepNode) {
    SscaOrchestrationStepInfo stepInfo = orchestrationStepNode.getSscaOrchestrationStepInfo();
    SbomSourceType sourceType = stepInfo.getSource().getType();
    switch (sourceType) {
      case REPOSITORY:
        if (Objects.nonNull(stepInfo.getAttestation())) {
          throw new InvalidYamlException("Orchestration step with Repo Source can't have Attestation field.");
        }
        if (stepInfo.getSbomDrift() != null
            && (stepInfo.getSbomDrift().getBase() == SbomDriftBase.LAST_GENERATED_SBOM
                || stepInfo.getSbomDrift().getBase() == SbomDriftBase.BASELINE)) {
          throw new InvalidYamlException(
              "Orchestration step with Repo Source can't have SbomDrift base field set to Baseline/Last_Generated_Sbom.");
        }
        break;
      case IMAGE:
        if (stepInfo.getSbomDrift() != null && stepInfo.getSbomDrift().getBase() == SbomDriftBase.REPOSITORY) {
          throw new InvalidYamlException(
              "Orchestration step with Image Source can't have SbomDrift base field set to Repository.");
        }
        break;
      default:
        break;
    }
  }

  public void validateEnforcementStep(SscaEnforcementStepNode enforcementStepNode) {
    SscaEnforcementStepInfo stepInfo = enforcementStepNode.getSscaEnforcementStepInfo();
    SbomSourceType sourceType = stepInfo.getSource().getType();
    switch (sourceType) {
      case REPOSITORY:
        if (Objects.nonNull(stepInfo.getVerifyAttestation())) {
          throw new InvalidYamlException("Enforcement step with Repo Source can't have Verify Attestation field.");
        }
        if (!EmptyPredicate.isEmpty(
                resolveStringParameter("source", SscaConstants.SSCA_ENFORCEMENT, stepInfo.getIdentifier(),
                    ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getClonedCodebase(), false))) {
          throw new InvalidYamlException("Enforcement step with Repo Source can't have Cloned Codebase field.");
        }
        break;
      default:
        break;
    }
  }
}
