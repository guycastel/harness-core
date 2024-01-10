/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import static io.harness.rule.OwnerRule.SRIDHAR;
import static io.harness.telemetry.helpers.InstrumentationConstants.ACCOUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.ORG;
import static io.harness.telemetry.helpers.InstrumentationConstants.PROJECT;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.CanaryVerificationJobSpec;
import io.harness.cvng.cdng.beans.MonitoredServiceSpecType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.instrumentation.CVNGTelemetryHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.jooq.tools.reflect.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CVNGTelemetryHelperTest extends CategoryTest {
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String IDENTIFIER = "identifier";

  private final String VERIFICATION_INSTANCE_ID = "verification_instance_id";
  @InjectMocks CVNGTelemetryHelper cvngTelemetryHelper;
  @Mock TelemetryReporter telemetryReporter;
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    Reflect.on(cvngTelemetryHelper).set("telemetryReporter", telemetryReporter);
  }

  @Test
  @Owner(developers = SRIDHAR)
  @Category(UnitTests.class)
  public void testPublishCVNGStepExecutionEvent() {
    CVNGStepTask cvngStepTask = CVNGStepTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .orgIdentifier(ORG_IDENTIFIER)
                                    .projectIdentifier(PROJ_IDENTIFIER)
                                    .serviceIdentifier(IDENTIFIER)
                                    .status(CVNGStepTask.Status.IN_PROGRESS)
                                    .deploymentStartTime(Instant.ofEpochMilli(System.currentTimeMillis()))
                                    .callbackId(VERIFICATION_INSTANCE_ID)
                                    .verificationJobInstanceId(VERIFICATION_INSTANCE_ID)
                                    .build();

    MonitoredServiceSpecType monitoredServiceSpecType = MonitoredServiceSpecType.DEFAULT;

    CVNGStepParameter cvngStepParameter =
        CVNGStepParameter.builder()
            .spec(CanaryVerificationJobSpec.builder()
                      .duration(ParameterField.<String>builder().value("5m").build())
                      .sensitivity(ParameterField.<String>builder().value("High").build())
                      .failOnNoAnalysis(ParameterField.<Boolean>builder().value(true).build())
                      .build())
            .build();
    CompletableFuture<Void> telemetryTask =
        cvngTelemetryHelper.publishCVNGStepExecutionEvent(cvngStepTask, monitoredServiceSpecType, cvngStepParameter);

    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);

    telemetryTask.join();
    verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), captor.capture(), any(), any(), any());

    HashMap<String, Object> eventPropertiesMap = captor.getValue();

    assert (eventPropertiesMap.get(ACCOUNT)).equals(ACCOUNT_ID);
    assert (eventPropertiesMap.get(ORG)).equals(ORG_IDENTIFIER);
    assert (eventPropertiesMap.get(PROJECT)).equals(PROJ_IDENTIFIER);
    assert (eventPropertiesMap.get(CVNGTelemetryHelper.DURATION)).equals("5m");
    assert (eventPropertiesMap.get(CVNGTelemetryHelper.FAIL_ON_NO_ANALYSIS)).equals(true);
    assert (eventPropertiesMap.get(CVNGTelemetryHelper.VERIFICATION_TYPE)).equals("Canary");
    assert (eventPropertiesMap.get(CVNGTelemetryHelper.SENSITIVITY)).equals("High");

    assertTrue(telemetryTask.isDone());
  }
}
