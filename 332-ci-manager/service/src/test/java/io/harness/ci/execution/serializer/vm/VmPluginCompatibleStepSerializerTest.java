/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.serializer.vm;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.rule.OwnerRule.DEVESH;
import static io.harness.rule.OwnerRule.RUTVIJ_MEHTA;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.stepinfo.DockerStepInfo;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.beans.sweepingoutputs.StageInfraDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.config.CIDockerLayerCachingConfig;
import io.harness.ci.execution.buildstate.PluginSettingUtils;
import io.harness.ci.execution.execution.CIDockerLayerCachingConfigService;
import io.harness.ci.execution.execution.CIExecutionConfigService;
import io.harness.ci.execution.integrationstage.IntegrationStageUtils;
import io.harness.ci.execution.serializer.SerializerUtils;
import io.harness.ci.execution.utils.CIStepInfoUtils;
import io.harness.ci.execution.utils.HarnessImageUtils;
import io.harness.ci.ff.CIFeatureFlagService;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.vm.steps.VmPluginStep;
import io.harness.delegate.beans.ci.vm.steps.VmRunStep;
import io.harness.idp.steps.beans.stepinfo.IdpCookieCutterStepInfo;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.timeout.Timeout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(CI)
public class VmPluginCompatibleStepSerializerTest {
  @Mock private CIFeatureFlagService ciFeatureFlagService;
  @Mock private PluginSettingUtils pluginSettingUtils;
  @Mock private CIDockerLayerCachingConfigService dockerLayerCachingConfigService;
  @Mock private CIExecutionConfigService ciExecutionConfigService;
  @InjectMocks private VmPluginCompatibleStepSerializer vmPluginStepSerializer;

  @Mock HarnessImageUtils harnessImageUtils;
  @Mock SerializerUtils serializerUtils;

  private static final String TEST_UUID_COOKIECUTTER = "test-cookie-cutter-uuid";
  private static final String TEST_IDENTIFIER_COOKIECUTTER = "test-cookie-cutter-identifier";
  private static final String TEST_NAME_COOKIECUTTER = "test-name-cookiecutter";
  private static final String TEST_PUBLIC_URL = "test-public-url";

  private static final String TEST_IMAGE_NAME = "test-image-name";

  private static final long TEST_TIMEOUT_VALUE = 10;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putAllSetupAbstractions(Maps.of(
            "accountId", "accountId", "projectIdentifier", "projectIdentfier", "orgIdentifier", "orgIdentifier"))
        .build();
  }

  private IdpCookieCutterStepInfo getCookieCutterStepInfo() {
    return IdpCookieCutterStepInfo.builder()
        .uuid(TEST_UUID_COOKIECUTTER)
        .identifier(TEST_IDENTIFIER_COOKIECUTTER)
        .name(TEST_NAME_COOKIECUTTER)
        .publicTemplateUrl(ParameterField.createValueField(TEST_PUBLIC_URL))
        .build();
  }

  private DockerStepInfo getDockerStepInfo() {
    return DockerStepInfo.builder()
        .repo(ParameterField.createValueField("harness"))
        .tags(ParameterField.createValueField(Arrays.asList("tag1", "tag2")))
        .dockerfile(ParameterField.createValueField("Dockerfile"))
        .context(ParameterField.createValueField("context"))
        .target(ParameterField.createValueField("target"))
        .build();
  }

  private ECRStepInfo getEcrStepInfo() {
    return ECRStepInfo.builder()
        .imageName(ParameterField.createValueField("harness"))
        .tags(ParameterField.createValueField(Arrays.asList("tag1", "tag2")))
        .dockerfile(ParameterField.createValueField("Dockerfile"))
        .context(ParameterField.createValueField("context"))
        .target(ParameterField.createValueField("target"))
        .build();
  }

  private CIDockerLayerCachingConfig getDlcConfig() {
    return CIDockerLayerCachingConfig.builder()
        .endpoint("endpoint")
        .bucket("bucket")
        .accessKey("access_key")
        .secretKey("secret_key")
        .region("region")
        .build();
  }

  @Test
  @Owner(developers = RUTVIJ_MEHTA)
  @Category(UnitTests.class)
  public void testPluginStepSerializerDockerDlcEnabled() {
    String accountId = "accountId";
    Ambiance ambiance = getAmbiance();
    DockerStepInfo dockerStepInfo = getDockerStepInfo();
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.DLITE_VM;
    CIDockerLayerCachingConfig config = getDlcConfig();

    when(ciFeatureFlagService.isEnabled(FeatureName.CI_HOSTED_CONTAINERLESS_OOTB_STEP_ENABLED, accountId))
        .thenReturn(true);
    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any(PluginCompatibleStep.class)))
        .thenReturn("pluginName");
    when(pluginSettingUtils.dlcSetupRequired(dockerStepInfo)).thenReturn(true);
    when(dockerLayerCachingConfigService.getDockerLayerCachingConfig(any(), anyBoolean())).thenReturn(config);

    Set<String> secretList =
        vmPluginStepSerializer.preProcessStep(ambiance, dockerStepInfo, stageInfraDetails, "identifier", false);
    assertThat(secretList)
        .contains("endpoint")
        .contains("bucket")
        .contains("access_key")
        .contains("secret_key")
        .contains("region");
  }

  @Test
  @Owner(developers = RUTVIJ_MEHTA)
  @Category(UnitTests.class)
  public void testPluginStepSerializerDockerDlcDisabled() {
    String accountId = "accountId";
    Ambiance ambiance = getAmbiance();
    DockerStepInfo dockerStepInfo = getDockerStepInfo();
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.DLITE_VM;

    when(ciFeatureFlagService.isEnabled(FeatureName.CI_HOSTED_CONTAINERLESS_OOTB_STEP_ENABLED, accountId))
        .thenReturn(true);
    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any(PluginCompatibleStep.class)))
        .thenReturn("pluginName");
    when(pluginSettingUtils.dlcSetupRequired(dockerStepInfo)).thenReturn(false);

    Set<String> secretList =
        vmPluginStepSerializer.preProcessStep(ambiance, dockerStepInfo, stageInfraDetails, "identifier", false);
    assertThat(secretList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RUTVIJ_MEHTA)
  @Category(UnitTests.class)
  public void testPluginStepSerializerDockerConfigNull() {
    String accountId = "accountId";
    Ambiance ambiance = getAmbiance();
    DockerStepInfo dockerStepInfo = getDockerStepInfo();
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.DLITE_VM;

    when(ciFeatureFlagService.isEnabled(FeatureName.CI_HOSTED_CONTAINERLESS_OOTB_STEP_ENABLED, accountId))
        .thenReturn(true);
    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any(PluginCompatibleStep.class)))
        .thenReturn("pluginName");
    when(pluginSettingUtils.dlcSetupRequired(dockerStepInfo)).thenReturn(true);
    when(dockerLayerCachingConfigService.getDockerLayerCachingConfig(any(), anyBoolean())).thenReturn(null);

    Set<String> secretList =
        vmPluginStepSerializer.preProcessStep(ambiance, dockerStepInfo, stageInfraDetails, "identifier", false);
    assertThat(secretList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RUTVIJ_MEHTA)
  @Category(UnitTests.class)
  public void testPluginStepSerializerDockerDlcEnabledEcr() {
    String accountId = "accountId";
    Ambiance ambiance = getAmbiance();
    ECRStepInfo ecrStepInfo = getEcrStepInfo();
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.DLITE_VM;
    CIDockerLayerCachingConfig config = getDlcConfig();

    when(ciFeatureFlagService.isEnabled(FeatureName.CI_HOSTED_CONTAINERLESS_OOTB_STEP_ENABLED, accountId))
        .thenReturn(true);
    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any(PluginCompatibleStep.class)))
        .thenReturn("pluginName");
    when(pluginSettingUtils.dlcSetupRequired(ecrStepInfo)).thenReturn(true);
    when(dockerLayerCachingConfigService.getDockerLayerCachingConfig(any(), anyBoolean())).thenReturn(config);

    Set<String> secretList =
        vmPluginStepSerializer.preProcessStep(ambiance, ecrStepInfo, stageInfraDetails, "identifier", false);
    assertThat(secretList)
        .contains("endpoint")
        .contains("bucket")
        .contains("access_key")
        .contains("secret_key")
        .contains("region");
  }

  @Test
  @Owner(developers = RUTVIJ_MEHTA)
  @Category(UnitTests.class)
  public void testPluginStepSerializerDockerDlcDisabledEcr() {
    String accountId = "accountId";
    Ambiance ambiance = getAmbiance();
    ECRStepInfo ecrStepInfo = getEcrStepInfo();
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.DLITE_VM;

    when(ciFeatureFlagService.isEnabled(FeatureName.CI_HOSTED_CONTAINERLESS_OOTB_STEP_ENABLED, accountId))
        .thenReturn(true);
    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any(PluginCompatibleStep.class)))
        .thenReturn("pluginName");
    when(pluginSettingUtils.dlcSetupRequired(ecrStepInfo)).thenReturn(false);

    Set<String> secretList =
        vmPluginStepSerializer.preProcessStep(ambiance, ecrStepInfo, stageInfraDetails, "identifier", false);
    assertThat(secretList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RUTVIJ_MEHTA)
  @Category(UnitTests.class)
  public void testPluginStepSerializerDockerConfigNullEcr() {
    String accountId = "accountId";
    Ambiance ambiance = getAmbiance();
    ECRStepInfo ecrStepInfo = getEcrStepInfo();
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.DLITE_VM;

    when(ciFeatureFlagService.isEnabled(FeatureName.CI_HOSTED_CONTAINERLESS_OOTB_STEP_ENABLED, accountId))
        .thenReturn(true);
    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any(PluginCompatibleStep.class)))
        .thenReturn("pluginName");
    when(pluginSettingUtils.dlcSetupRequired(ecrStepInfo)).thenReturn(true);
    when(dockerLayerCachingConfigService.getDockerLayerCachingConfig(any(), anyBoolean())).thenReturn(null);

    Set<String> secretList =
        vmPluginStepSerializer.preProcessStep(ambiance, ecrStepInfo, stageInfraDetails, "identifier", false);
    assertThat(secretList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = DEVESH)
  @Category(UnitTests.class)
  public void testSerializeByExcludingConnector() {
    Map<String, String> envVariables = new HashMap<>();

    String testEnvName = "testEnvName";
    String testEnvValue = "testEnvValue";

    envVariables.put(testEnvName, testEnvValue);
    StageInfraDetails stageInfraDetails = () -> StageInfraDetails.Type.K8;

    when(pluginSettingUtils.getPluginCompatibleEnvVariables(
             any(), any(), anyLong(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(envVariables);
    when(serializerUtils.getStepStatusEnvVars(any())).thenReturn(envVariables);

    Mockito.mockStatic(TimeoutUtils.class);
    when(TimeoutUtils.getTimeoutInSeconds((ParameterField<Timeout>) Mockito.any(), Mockito.anyLong()))
        .thenReturn(TEST_TIMEOUT_VALUE);

    Mockito.mockStatic(CIStepInfoUtils.class);
    when(CIStepInfoUtils.getPluginCustomStepImage(any(), any(), any(), any())).thenReturn(TEST_IMAGE_NAME);

    when(harnessImageUtils.getHarnessImageConnectorDetailsForVM(any(), any()))
        .thenReturn(ConnectorDetails.builder().build());

    Mockito.mockStatic(IntegrationStageUtils.class);
    when(IntegrationStageUtils.getFullyQualifiedImageName(any(), any())).thenReturn(TEST_IMAGE_NAME);

    VmPluginStep vmPluginStep = (VmPluginStep) vmPluginStepSerializer.serializeByExcludingConnector(getAmbiance(),
        getCookieCutterStepInfo(), stageInfraDetails, TEST_IDENTIFIER_COOKIECUTTER,
        ParameterField.createValueField(Timeout.builder().build()), TEST_NAME_COOKIECUTTER);
    assertNull(vmPluginStep.getConnector());
    assertEquals(vmPluginStep.getEnvVariables().get(testEnvName), testEnvValue);

    // test for container less plugins steps
    when(CIStepInfoUtils.canRunVmStepOnHost(any(), any(), any(), any(), any(), any())).thenReturn(true);
    String testName = "testName";

    when(pluginSettingUtils.getPluginCompatibleEnvVariables(
             any(), any(), anyLong(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(envVariables);

    when(ciExecutionConfigService.getContainerlessPluginNameForVM(any(), any())).thenReturn(testName);
    VmRunStep vmRunStep = (VmRunStep) vmPluginStepSerializer.serializeByExcludingConnector(getAmbiance(),
        getCookieCutterStepInfo(), stageInfraDetails, TEST_IDENTIFIER_COOKIECUTTER,
        ParameterField.createValueField(Timeout.builder().build()), TEST_NAME_COOKIECUTTER);
    assertNull(vmRunStep.getConnector());
    assertEquals(vmRunStep.getEnvVariables().get(testEnvName), testEnvValue);
  }
}
