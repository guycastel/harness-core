/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.WAIT_STEP_RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.NodeStatusUpdateHandler;
import io.harness.engine.observers.NodeUpdateInfo;

import com.google.inject.Inject;

@OwnedBy(PIPELINE)
public class WaitStepStatusUpdate implements NodeStatusUpdateHandler {
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleNodeStatusUpdate(NodeUpdateInfo nodeStatusUpdateInfo) {
    planExecutionService.updateStatus(nodeStatusUpdateInfo.getPlanExecutionId(), WAIT_STEP_RUNNING);
  }
}
