/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.barriers.event;

import static io.harness.rule.OwnerRule.VINICIUS;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.lock.PersistentLocker;
import io.harness.lock.noop.AcquiredNoopLock;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.steps.barriers.BarrierSpecParameters;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPosition.BarrierPositionType;
import io.harness.steps.barriers.event.BarrierEventHandler;
import io.harness.steps.barriers.service.BarrierService;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierEventHandlerTest extends OrchestrationStepsTestBase {
  @Mock BarrierService barrierService;
  @Mock PersistentLocker persistentLocker;
  @InjectMocks BarrierEventHandler barrierEventHandler;

  @Test
  @Owner(developers = VINICIUS)
  @Category(UnitTests.class)
  public void testOnNodeStatusUpdateBarrier() {
    String accountId = "accountId";
    String executionId = "executionId";
    String planExecutionId = "planExecutionId";
    String barrierRef = "barrierRef";
    BarrierExecutionInstance barrierExecutionInstance = BarrierExecutionInstance.builder().build();
    NodeUpdateInfo nodeUpdateInfo =
        NodeUpdateInfo.builder()
            .nodeExecution(NodeExecution.builder()
                               .uuid(executionId)
                               .ambiance(Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build())
                               .status(Status.ASYNC_WAITING)
                               .build())
            .build();
    when(persistentLocker.waitToAcquireLock(any(), any(), any())).thenReturn(AcquiredNoopLock.builder().build());
    when(barrierService.updatePosition(
             planExecutionId, BarrierPositionType.STEP, "setupId", executionId, "stageId", "stepGroupId"))
        .thenReturn(List.of());
    when(barrierService.findByIdentifierAndPlanExecutionId(barrierRef, planExecutionId))
        .thenReturn(barrierExecutionInstance);
    try (MockedStatic<AmbianceUtils> mockAmbianceUtils = mockStatic(AmbianceUtils.class, RETURNS_MOCKS);
         MockedStatic<RecastOrchestrationUtils> mockRecastOrchestrationUtils =
             mockStatic(RecastOrchestrationUtils.class, RETURNS_MOCKS)) {
      when(AmbianceUtils.getAccountId(any())).thenReturn(accountId);
      when(AmbianceUtils.obtainCurrentLevel(any()))
          .thenReturn(Level.newBuilder().setGroup(BarrierPositionType.STEP.name()).setSetupId("setupId").build());
      when(AmbianceUtils.getStageLevelFromAmbiance(any()))
          .thenReturn(Optional.of(Level.newBuilder().setRuntimeId("stageId").build()));
      when(AmbianceUtils.getStepGroupLevelFromAmbiance(any()))
          .thenReturn(Optional.of(Level.newBuilder().setRuntimeId("stepGroupId").build()));
      when(AmbianceUtils.getCurrentStepType(any())).thenReturn(BarrierStep.STEP_TYPE);
      when(RecastOrchestrationUtils.fromMap(any(), any()))
          .thenReturn(StepElementParameters.builder()
                          .spec(BarrierSpecParameters.builder().barrierRef(barrierRef).build())
                          .build());
      barrierEventHandler.onNodeStatusUpdate(nodeUpdateInfo);
      verify(barrierService, times(1))
          .updatePosition(planExecutionId, BarrierPositionType.STEP, "setupId", executionId, "stageId", "stepGroupId");
    }
    verify(barrierService, times(1)).findByIdentifierAndPlanExecutionId(barrierRef, planExecutionId);
    verify(barrierService, times(1)).update(barrierExecutionInstance);
  }
}
