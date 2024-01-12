/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.execution.orchestration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.pod.EnvVariableEnum;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.ssca.beans.OrchestrationStepEnvVariables;
import io.harness.ssca.beans.OrchestrationStepSecretVariables;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.SSCA)
@UtilityClass
public class SscaOrchestrationStepPluginUtils {
  public static final String PLUGIN_TOOL = "PLUGIN_TOOL";
  public static final String PLUGIN_FORMAT = "PLUGIN_FORMAT";
  public static final String PLUGIN_REPO_SBOMSOURCE_URL = "PLUGIN_REPO_SBOMSOURCE_URL";
  public static final String PLUGIN_REPO_SBOMSOURCE_PATH = "PLUGIN_REPO_SBOMSOURCE_PATH";
  public static final String PLUGIN_REPO_SBOMSOURCE_VARIANT = "PLUGIN_REPO_SBOMSOURCE_VARIANT";
  public static final String PLUGIN_REPO_SBOMSOURCE_VARIANT_TYPE = "PLUGIN_REPO_SBOMSOURCE_VARIANT_TYPE";
  public static final String PLUGIN_REPO_SBOMSOURCE_CLONED_CODEBASE = "PLUGIN_REPO_SBOMSOURCE_CLONED_CODEBASE";

  public static final String PLUGIN_SBOMSOURCE = "PLUGIN_SBOMSOURCE";

  public static final String PLUGIN_SBOMSOURCE_TYPE = "PLUGIN_SBOMSOURCE_TYPE";
  public static final String PLUGIN_TYPE = "PLUGIN_TYPE";
  public static final String PLUGIN_MODE = "PLUGIN_MODE";
  public static final String PLUGIN_SBOMDESTINATION = "PLUGIN_SBOMDESTINATION";
  public static final String SKIP_NORMALISATION = "SKIP_NORMALISATION";
  public static final String COSIGN_PASSWORD = "COSIGN_PASSWORD";
  public static final String SSCA_CORE_URL = "SSCS_Core_Url";
  public static final String STEP_EXECUTION_ID = "STEP_EXECUTION_ID";
  public static final String STEP_ID = "STEP_ID";
  public static final String DOCKER_USERNAME = "DOCKER_USERNAME";
  public static final String DOCKER_PASSW = "DOCKER_PASSWORD";
  public static final String DOCKER_REGISTRY = "DOCKER_REGISTRY";
  public static final String COSIGN_PRIVATE_KEY = "COSIGN_PRIVATE_KEY";
  public static final String SSCA_MANAGER_ENABLED = "SSCA_MANAGER_ENABLED";
  public static final String PLUGIN_SBOM_DRIFT = "PLUGIN_SBOM_DRIFT";
  public static final String PLUGIN_SBOM_DRIFT_VARIANT = "PLUGIN_SBOM_DRIFT_VARIANT";
  public static final String PLUGIN_SBOM_DRIFT_VARIANT_TYPE = "PLUGIN_SBOM_DRIFT_VARIANT_TYPE";

  public static final String PLUGIN_BASE64_SECRET = "PLUGIN_BASE64_SECRET";
  public static final String ENABLE_SSCA_AIRGAP = "ENABLE_SSCA_AIRGAP";

  public Map<String, String> getSScaOrchestrationStepEnvVariables(OrchestrationStepEnvVariables envVariables) {
    Map<String, String> envMap = new HashMap<>();
    envMap.put(PLUGIN_TOOL, envVariables.getSbomGenerationTool());
    envMap.put(PLUGIN_FORMAT, envVariables.getSbomGenerationFormat());
    envMap.put(PLUGIN_SBOMSOURCE_TYPE, envVariables.getSbomSourceType());
    envMap.put(PLUGIN_SBOMSOURCE, envVariables.getSbomSource());
    envMap.put(PLUGIN_REPO_SBOMSOURCE_URL, envVariables.getRepoUrl());
    envMap.put(PLUGIN_REPO_SBOMSOURCE_PATH, envVariables.getRepoPath());
    envMap.put(PLUGIN_REPO_SBOMSOURCE_VARIANT, envVariables.getRepoVariant());
    envMap.put(PLUGIN_REPO_SBOMSOURCE_VARIANT_TYPE, envVariables.getRepoVariantType());
    envMap.put(PLUGIN_REPO_SBOMSOURCE_CLONED_CODEBASE, envVariables.getRepoClonedCodebasePath());
    envMap.put(PLUGIN_TYPE, "Orchestrate");
    String SbomMode = envVariables.getSbomMode();
    envMap.put(PLUGIN_MODE, SbomMode == null ? "generation" : SbomMode);
    String defaultSbomDestination = "harness/sbom";
    String SbomDestination = envVariables.getSbomDestination();
    envMap.put(PLUGIN_SBOMDESTINATION, SbomDestination == null ? defaultSbomDestination : SbomDestination);
    envMap.put(SKIP_NORMALISATION, "true");
    envMap.put(SSCA_CORE_URL, envVariables.getSscaCoreUrl());
    envMap.put(SSCA_MANAGER_ENABLED, String.valueOf(envVariables.isSscaManagerEnabled()));
    envMap.put(STEP_EXECUTION_ID, envVariables.getStepExecutionId());
    envMap.put(STEP_ID, envVariables.getStepIdentifier());
    envMap.put(PLUGIN_SBOM_DRIFT, envVariables.getSbomDrift());
    envMap.put(PLUGIN_SBOM_DRIFT_VARIANT, envVariables.getSbomDriftVariant());
    envMap.put(PLUGIN_SBOM_DRIFT_VARIANT_TYPE, envVariables.getSbomDriftVariantType());
    envMap.put(PLUGIN_BASE64_SECRET, String.valueOf(envVariables.isBase64SecretAttestation()));
    envMap.put(ENABLE_SSCA_AIRGAP, String.valueOf(envVariables.isAirgapEnabled()));
    envMap.values().removeAll(Collections.singleton(null));

    return envMap;
  }

  public Map<String, SecretNGVariable> getSscaOrchestrationSecretVars(
      OrchestrationStepSecretVariables secretVariables) {
    Map<String, SecretNGVariable> secretNGVariableMap = new HashMap<>();
    if (secretVariables == null) {
      return secretNGVariableMap;
    }
    if (secretVariables.getAttestationPrivateKey() != null) {
      SecretRefData secretRefData = SecretRefHelper.createSecretRef(secretVariables.getAttestationPrivateKey());
      secretNGVariableMap.put(COSIGN_PRIVATE_KEY,
          SecretNGVariable.builder()
              .type(NGVariableType.SECRET)
              .value(ParameterField.createValueField(secretRefData))
              .name(COSIGN_PRIVATE_KEY)
              .build());
    }
    if (secretVariables.getCosignPassword() != null) {
      SecretRefData secretRefData = SecretRefHelper.createSecretRef(secretVariables.getCosignPassword());
      secretNGVariableMap.put(COSIGN_PASSWORD,
          SecretNGVariable.builder()
              .type(NGVariableType.SECRET)
              .value(ParameterField.createValueField(secretRefData))
              .name(COSIGN_PASSWORD)
              .build());
    }
    return secretNGVariableMap;
  }

  public static Map<EnvVariableEnum, String> getConnectorSecretEnvMap() {
    Map<EnvVariableEnum, String> map = new HashMap<>();
    map.put(EnvVariableEnum.DOCKER_USERNAME, DOCKER_USERNAME);
    map.put(EnvVariableEnum.DOCKER_PASSWORD, DOCKER_PASSW);
    map.put(EnvVariableEnum.DOCKER_REGISTRY, DOCKER_REGISTRY);
    return map;
  }
}
