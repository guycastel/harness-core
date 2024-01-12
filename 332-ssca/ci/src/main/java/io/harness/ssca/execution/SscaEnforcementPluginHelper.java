/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.execution;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveListParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.sweepingoutputs.StageInfraDetails.Type;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.ssca.beans.SscaConstants;
import io.harness.ssca.beans.attestation.AttestationType;
import io.harness.ssca.beans.attestation.verify.CosignVerifyAttestation;
import io.harness.ssca.beans.enforcement.EnforcementStepEnvVariables;
import io.harness.ssca.beans.enforcement.EnforcementStepSecretVariables;
import io.harness.ssca.beans.source.ImageSbomSource;
import io.harness.ssca.beans.source.RepositorySbomSource;
import io.harness.ssca.beans.source.SbomSourceType;
import io.harness.ssca.beans.stepinfo.SscaEnforcementStepInfo;
import io.harness.ssca.beans.store.HarnessStore;
import io.harness.ssca.beans.store.StoreType;
import io.harness.ssca.client.NgSettingsUtils;
import io.harness.ssca.client.SSCAServiceUtils;
import io.harness.ssca.execution.enforcement.SscaEnforcementStepPluginUtils;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;

@OwnedBy(HarnessTeam.SSCA)
public class SscaEnforcementPluginHelper {
  @Inject private SSCAServiceUtils sscaServiceUtils;
  @Inject private NgSettingsUtils ngSettingsUtils;

  public Map<String, String> getSscaEnforcementStepEnvVariables(
      SscaEnforcementStepInfo stepInfo, String identifier, Ambiance ambiance, Type infraType) {
    String sbomSourceType = stepInfo.getSource().getType().toString();
    String sbomSource = null;
    if (SbomSourceType.IMAGE.equals(stepInfo.getSource().getType())) {
      sbomSource = resolveStringParameter("source", SscaConstants.SSCA_ENFORCEMENT, identifier,
          ((ImageSbomSource) stepInfo.getSource().getSbomSourceSpec()).getImage(), true);
    }

    String repoUrl = null;
    String repoPath = null;
    String repoVariant = null;
    String repoVariantType = null;
    if (SbomSourceType.REPOSITORY.equals(stepInfo.getSource().getType())) {
      repoUrl = resolveStringParameter("source", SscaConstants.SSCA_ENFORCEMENT, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getUrl(), true);
      repoPath = resolveStringParameter("source", SscaConstants.SSCA_ENFORCEMENT, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getPath(), true);
      repoVariant = resolveStringParameter("source", SscaConstants.SSCA_ENFORCEMENT, identifier,
          ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getVariant(), true);
      repoVariantType = ((RepositorySbomSource) stepInfo.getSource().getSbomSourceSpec()).getVariantType().toString();
    }

    String policyFile = null;
    if (stepInfo.getPolicy().getStore() != null
        && StoreType.HARNESS.equals(stepInfo.getPolicy().getStore().getType())) {
      policyFile = resolveStringParameter("file", SscaConstants.SSCA_ENFORCEMENT, identifier,
          ((HarnessStore) stepInfo.getPolicy().getStore().getStoreSpec()).getFile(), false);
    }

    String policySetRef = null;
    if (stepInfo.getPolicy().getPolicySets() != null) {
      List<String> policySetRefs = resolveListParameter(
          "policySets", SscaConstants.SSCA_ENFORCEMENT, identifier, stepInfo.getPolicy().getPolicySets(), false);
      if (CollectionUtils.isNotEmpty(policySetRefs)) {
        policySetRef = policySetRefs.toString().replace("[", "").replace("]", "").replace(" ", "");
      }
    }
    String runtimeId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);

    boolean useBase64SecretForAttestation = ngSettingsUtils.getBaseEncodingEnabled(AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

    boolean airgapEnabled = ngSettingsUtils.getAirgapEnabled(AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

    Map<String, String> envMap = SscaEnforcementStepPluginUtils.getSscaEnforcementStepEnvVariables(
        EnforcementStepEnvVariables.builder()
            .stepExecutionId(runtimeId)
            .sbomSourceType(sbomSourceType)
            .sbomSource(sbomSource)
            .repoUrl(repoUrl)
            .repoPath(repoPath)
            .repoVariant(repoVariant)
            .repoVariantType(repoVariantType)
            .harnessPolicyFileId(policyFile)
            .sscaManagerEnabled(sscaServiceUtils.isSSCAManagerEnabled())
            .policySetRef(policySetRef)
            .base64SecretAttestation(useBase64SecretForAttestation)
            .airgapEnabled(airgapEnabled)
            .build());
    if (infraType == Type.VM) {
      envMap.putAll(getSscaEnforcementSecretEnvMap(stepInfo, identifier, ambiance.getExpressionFunctorToken()));
    }
    return envMap;
  }

  private Map<String, String> getSscaEnforcementSecretEnvMap(
      SscaEnforcementStepInfo stepInfo, String identifier, long expressionFunctorToken) {
    Map<String, String> envMap = new HashMap<>();
    if (stepInfo.getVerifyAttestation() != null
        && AttestationType.COSIGN.equals(stepInfo.getVerifyAttestation().getType())) {
      CosignVerifyAttestation verifyAttestation =
          (CosignVerifyAttestation) stepInfo.getVerifyAttestation().getVerifyAttestationSpec();
      String cosignPublicKey = resolveStringParameter(
          "publicKey", SscaConstants.SSCA_ENFORCEMENT, identifier, verifyAttestation.getPublicKey(), true);
      envMap.put(SscaEnforcementStepPluginUtils.COSIGN_PUBLIC_KEY,
          NGVariablesUtils.fetchSecretExpressionWithExpressionToken(cosignPublicKey, expressionFunctorToken));
    }
    return envMap;
  }

  public Map<String, SecretNGVariable> getSscaEnforcementSecretVariables(
      SscaEnforcementStepInfo stepInfo, String identifier) {
    if (stepInfo.getVerifyAttestation() != null
        && AttestationType.COSIGN.equals(stepInfo.getVerifyAttestation().getType())) {
      CosignVerifyAttestation verifyAttestation =
          (CosignVerifyAttestation) stepInfo.getVerifyAttestation().getVerifyAttestationSpec();
      String cosignPublicKey = resolveStringParameter(
          "publicKey", SscaConstants.SSCA_ENFORCEMENT, identifier, verifyAttestation.getPublicKey(), true);
      return SscaEnforcementStepPluginUtils.getSscaEnforcementSecretVars(
          EnforcementStepSecretVariables.builder().cosignPublicKey(cosignPublicKey).build());
    }
    return new HashMap<>();
  }
}
