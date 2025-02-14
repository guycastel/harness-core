/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InterruptPackage;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressPackage;
import io.harness.pms.sdk.core.steps.Step;

@OwnedBy(HarnessTeam.PIPELINE)
public class DummyExecutionStrategy implements ExecuteStrategy {
  public DummyExecutionStrategy() {}

  @Override
  public void start(InvokerPackage invokerPackage) {
    // do Nothing
  }

  @Override
  public <T extends Step> T extractStep(Ambiance ambiance) {
    return null;
  }

  @Override
  public void progress(ProgressPackage progressPackage) {
    // do Nothing
  }

  @Override
  public void abort(InterruptPackage interruptPackage) {}

  @Override
  public void expire(InterruptPackage interruptPackage) {}

  @Override
  public void failure(InterruptPackage interruptPackage) {}
}
