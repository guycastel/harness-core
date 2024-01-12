/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.execution;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.sweepingoutputs.StageInfraDetails.Type;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ngexception.CIStageExecutionUserException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.ssca.beans.OrchestrationStepEnvVariables;
import io.harness.ssca.beans.OrchestrationStepSecretVariables;
import io.harness.ssca.beans.SscaConstants;
import io.harness.ssca.beans.attestation.AttestationType;
import io.harness.ssca.beans.attestation.CosignAttestation;
import io.harness.ssca.beans.sbomDrift.RepositorySbomDrift;
import io.harness.ssca.beans.sbomDrift.SbomDriftBase;
import io.harness.ssca.beans.source.ImageSbomSource;
import io.harness.ssca.beans.source.RepositorySbomSource;
import io.harness.ssca.beans.source.SbomSourceType;
import io.harness.ssca.beans.stepinfo.SscaOrchestrationStepInfo;
import io.harness.ssca.beans.tools.SbomOrchestrationToolType;
import io.harness.ssca.beans.tools.syft.SyftSbomOrchestration;
import io.harness.ssca.client.NgSettingsUtils;
import io.harness.ssca.client.SSCAServiceUtils;
import io.harness.ssca.execution.orchestration.SscaOrchestrationStepPluginUtils;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.SSCA)
public class SscaOrchestrationPluginUtils {
  @Inject private SSCAServiceUtils sscaServiceUtils;
  @Inject private NgSettingsUtils ngSettingsUtils;

  public Map<String, String> getSscaOrchestrationStepEnvVariables(
      SscaOrchestrationStepInfo stepInfo, String identifier, Ambiance ambiance, Type type) {
    String tool = null;
    if (stepInfo.getTool() != null && stepInfo.getTool().getType() != null) {
      tool = stepInfo.getTool().getType().toString();
    }
    String format = getFormat(stepInfo);

    // set to generation by default for backwards compatibility
    String mode = "generation";
    if (stepInfo.getMode() != null) {
      mode = stepInfo.getMode().toString();
    }

    String ingestion = null;
    if (stepInfo.getIngestion() != null && stepInfo.getIngestion().getFile() != null) {
      ingestion = stepInfo.getIngestion().getFile().getValue();
    }
    String sbomSourceType = stepInfo.getSource().getType().toString();
    String sbomSource = null;
    if (SbomSourceType.IMAGE.equals(stepInfo.getSource().getType())) {
      sbomSource = resolveStringParameter("source", SscaConstants.SSCA_ORCHESTRATION_STEP, identifier,
          ((ImageSbomSource) stepInfo.getSource().getSbomSourceSpec()).getImage(), true);
    }

    String repoUrl = null;
    String repoPath = null;
    String repoVariant = null;
    String repoVariantType = null;
    String repoClonedCodebasePath = null;
    if (SbomSourceType.REPOSITORY.equals(stepInfo.getSource().getType())) {
      repoUrl = resolveStringParameter("source", SscaConstants.SSCA_ORCHESTRATION_STEP, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getUrl(), true);
      repoPath = resolveStringParameter("source", SscaConstants.SSCA_ORCHESTRATION_STEP, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getPath(), true);
      repoVariant = resolveStringParameter("source", SscaConstants.SSCA_ORCHESTRATION_STEP, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getVariant(), true);
      repoVariantType = ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getVariantType().toString();
      repoClonedCodebasePath = resolveStringParameter("source", SscaConstants.SSCA_ORCHESTRATION_STEP, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getClonedCodebase(), true);
    }

    String sbomDrift = null;
    String sbomDriftVariant = null;
    String sbomDriftVariantType = null;
    if (stepInfo.getSbomDrift() != null && stepInfo.getSbomDrift().getBase() != null) {
      sbomDrift = stepInfo.getSbomDrift().getBase().toString();
      if (stepInfo.getSbomDrift().getBase().equals(SbomDriftBase.REPOSITORY)
          && stepInfo.getSbomDrift().getSbomDriftSpec() != null) {
        sbomDriftVariant = resolveStringParameter("source", SscaConstants.SSCA_ORCHESTRATION_STEP, identifier,
            ((RepositorySbomDrift) stepInfo.getSbomDrift().getSbomDriftSpec()).getVariant(), true);
        sbomDriftVariantType =
            ((RepositorySbomDrift) stepInfo.getSbomDrift().getSbomDriftSpec()).getVariantType().toString();
      }
    }

    boolean useBase64SecretForAttestation = ngSettingsUtils.getBaseEncodingEnabled(AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

    boolean airgapEnabled = ngSettingsUtils.getAirgapEnabled(AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

    String runtimeId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    OrchestrationStepEnvVariables envVariables =
        OrchestrationStepEnvVariables.builder()
            .sbomGenerationTool(tool)
            .sbomGenerationFormat(format)
            .sbomSourceType(sbomSourceType)
            .sbomSource(sbomSource)
            .repoUrl(repoUrl)
            .repoPath(repoPath)
            .repoVariant(repoVariant)
            .repoVariantType(repoVariantType)
            .repoClonedCodebasePath(repoClonedCodebasePath != null ? repoClonedCodebasePath : "/harness")
            .sscaCoreUrl(sscaServiceUtils.getSscaServiceConfig().getHttpClientConfig().getBaseUrl())
            .sbomMode(mode)
            .sbomDestination(ingestion)
            .stepExecutionId(runtimeId)
            .stepIdentifier(identifier)
            .sscaManagerEnabled(sscaServiceUtils.getSscaServiceConfig().isSscaManagerEnabled())
            .sbomDrift(sbomDrift)
            .sbomDriftVariant(sbomDriftVariant)
            .sbomDriftVariantType(sbomDriftVariantType)
            .base64SecretAttestation(useBase64SecretForAttestation)
            .airgapEnabled(airgapEnabled)
            .build();
    Map<String, String> envMap = SscaOrchestrationStepPluginUtils.getSScaOrchestrationStepEnvVariables(envVariables);
    if (type == Type.VM) {
      envMap.putAll(getSscaOrchestrationSecretEnvMap(stepInfo, ambiance.getExpressionFunctorToken()));
    }
    return envMap;
  }

  public static Map<String, SecretNGVariable> getSscaOrchestrationSecretVars(SscaOrchestrationStepInfo stepInfo) {
    Map<String, SecretNGVariable> secretNGVariableMap = new HashMap<>();
    if (stepInfo.getAttestation() != null && AttestationType.COSIGN.equals(stepInfo.getAttestation().getType())) {
      CosignAttestation cosignAttestation = (CosignAttestation) stepInfo.getAttestation().getAttestationSpec();
      OrchestrationStepSecretVariables secretVariables = OrchestrationStepSecretVariables.builder()
                                                             .attestationPrivateKey(cosignAttestation.getPrivateKey())
                                                             .cosignPassword(cosignAttestation.getPassword())
                                                             .build();
      return SscaOrchestrationStepPluginUtils.getSscaOrchestrationSecretVars(secretVariables);
    }
    return secretNGVariableMap;
  }

  private Map<String, String> getSscaOrchestrationSecretEnvMap(
      SscaOrchestrationStepInfo stepInfo, long expressionFunctorToken) {
    Map<String, String> secretEnvMap = new HashMap<>();
    if (stepInfo.getAttestation() != null && AttestationType.COSIGN.equals(stepInfo.getAttestation().getType())) {
      CosignAttestation cosignAttestation = (CosignAttestation) stepInfo.getAttestation().getAttestationSpec();
      if (EmptyPredicate.isNotEmpty(cosignAttestation.getPrivateKey())) {
        secretEnvMap.put(SscaOrchestrationStepPluginUtils.COSIGN_PRIVATE_KEY,
            NGVariablesUtils.fetchSecretExpressionWithExpressionToken(
                cosignAttestation.getPrivateKey(), expressionFunctorToken));
      }
      if (EmptyPredicate.isNotEmpty(cosignAttestation.getPassword())) {
        secretEnvMap.put(SscaOrchestrationStepPluginUtils.COSIGN_PASSWORD,
            NGVariablesUtils.fetchSecretExpressionWithExpressionToken(
                cosignAttestation.getPassword(), expressionFunctorToken));
      }
    }
    return secretEnvMap;
  }

  private static String getFormat(SscaOrchestrationStepInfo stepInfo) {
    if (stepInfo.getTool() == null) {
      return null;
    }
    if (Objects.requireNonNull(stepInfo.getTool().getType()) == SbomOrchestrationToolType.SYFT) {
      return ((SyftSbomOrchestration) stepInfo.getTool().getSbomOrchestrationSpec()).getFormat().toString();
    }
    throw new CIStageExecutionUserException(String.format("Unsupported tool type: %s", stepInfo.getTool().getType()));
  }
}
