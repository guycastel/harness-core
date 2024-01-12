/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.service.steps;

import static io.harness.cdng.hooks.ServiceHookConstants.FETCH_FILES;
import static io.harness.cdng.hooks.ServiceHookConstants.POST_HOOK;
import static io.harness.cdng.hooks.ServiceHookConstants.PRE_HOOK;
import static io.harness.cdng.hooks.ServiceHookConstants.SERVICE_HOOK_ACTIONS;
import static io.harness.cdng.hooks.ServiceHookConstants.SERVICE_HOOK_TYPES;
import static io.harness.cdng.hooks.ServiceHookConstants.STEADY_STATE_CHECK;
import static io.harness.cdng.hooks.ServiceHookConstants.TEMPLATE_MANIFEST;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.hooks.ServiceHook;
import io.harness.cdng.hooks.ServiceHookAction;
import io.harness.cdng.hooks.ServiceHookWrapper;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.ng.core.service.yaml.NGServiceConfig;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class ServiceStepOverrideHelperTest extends CategoryTest {
  @Mock private ExecutionSweepingOutputService sweepingOutputService;
  private AutoCloseable mocks;

  @InjectMocks private ServiceStepOverrideHelper serviceStepOverrideHelper = new ServiceStepOverrideHelper();

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  @Owner(developers = OwnerRule.TMACARI)
  @Category(UnitTests.class)
  public void testPrepareAndSaveFinalServiceHooksMetadataToSweepingOutput() {
    HashMap<String, Object> properties = new HashMap<>();
    List<ServiceHookWrapper> hooks = new ArrayList<>();
    hooks.add(
        ServiceHookWrapper.builder()
            .postHook(ServiceHook.builder()
                          .identifier("id1")
                          .actions(Arrays.asList(ServiceHookAction.FETCH_FILES, ServiceHookAction.STEADY_STATE_CHECK))
                          .build())
            .build());
    hooks.add(
        ServiceHookWrapper.builder()
            .preHook(ServiceHook.builder()
                         .identifier("id2")
                         .actions(Arrays.asList(ServiceHookAction.FETCH_FILES, ServiceHookAction.TEMPLATE_MANIFEST))
                         .build())
            .build());
    ServiceDefinition serviceDefinition = new ServiceDefinition();
    serviceDefinition.setType(ServiceDefinitionType.KUBERNETES);
    serviceDefinition.setServiceSpec(KubernetesServiceSpec.builder().hooks(hooks).build());

    serviceStepOverrideHelper.prepareAndSaveFinalServiceHooksMetadataToSweepingOutput(
        NGServiceConfig.builder()
            .ngServiceV2InfoConfig(NGServiceV2InfoConfig.builder()
                                       .name("service1")
                                       .identifier("id")
                                       .serviceDefinition(serviceDefinition)
                                       .build())
            .build(),
        Ambiance.newBuilder().build(), "sweepingOutputName", properties);
    HashSet<String> actions = (HashSet<String>) properties.get(SERVICE_HOOK_ACTIONS);
    HashSet<String> types = (HashSet<String>) properties.get(SERVICE_HOOK_TYPES);

    verify(sweepingOutputService).consume(any(), any(), any(), any());
    assertThat(actions).contains(FETCH_FILES, TEMPLATE_MANIFEST, STEADY_STATE_CHECK);
    assertThat(types).contains(PRE_HOOK, POST_HOOK);
  }
}
