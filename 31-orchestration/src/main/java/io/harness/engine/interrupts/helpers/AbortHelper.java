package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionUpdateFailedException;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.Abortable;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.facilitator.modes.TaskSpawningExecutableResponse;
import io.harness.interrupts.Interrupt;
import io.harness.plan.PlanNode;
import io.harness.registries.state.StepRegistry;
import io.harness.state.Step;
import io.harness.tasks.TaskExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Slf4j
public class AbortHelper {
  @Inject private StepRegistry stepRegistry;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private OrchestrationEngine engine;

  public void discontinueMarkedInstance(NodeExecution nodeExecution, Status finalStatus) {
    try {
      Ambiance ambiance = nodeExecution.getAmbiance();
      PlanNode node = nodeExecution.getNode();
      Step currentState = Preconditions.checkNotNull(stepRegistry.obtain(node.getStepType()));
      ExecutableResponse executableResponse = nodeExecution.obtainLatestExecutableResponse();
      if (executableResponse != null && nodeExecution.isTaskSpawningMode()) {
        TaskSpawningExecutableResponse taskExecutableResponse = (TaskSpawningExecutableResponse) executableResponse;
        TaskExecutor executor = taskExecutorMap.get(taskExecutableResponse.getTaskMode().name());
        boolean aborted = executor.abortTask(ambiance.getSetupAbstractions(), taskExecutableResponse.getTaskId());
        if (!aborted) {
          logger.error("Delegate Task Cannot be aborted : TaskId: {}, NodeExecutionId: {}",
              taskExecutableResponse.getTaskId(), nodeExecution.getUuid());
        }
      }
      if (currentState instanceof Abortable) {
        ((Abortable) currentState).handleAbort(ambiance, nodeExecution.getResolvedStepParameters(), executableResponse);
      }

      NodeExecution updatedNodeExecution = nodeExecutionService.update(nodeExecution.getUuid(),
          ops
          -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis()).set(NodeExecutionKeys.status, finalStatus));
      engine.endTransition(updatedNodeExecution, finalStatus, null, null);
    } catch (NodeExecutionUpdateFailedException ex) {
      throw new InterruptProcessingFailedException(ABORT_ALL,
          "Abort failed for execution Plan :" + nodeExecution.getAmbiance().getPlanExecutionId()
              + "for NodeExecutionId: " + nodeExecution.getUuid(),
          ex);
    } catch (Exception e) {
      logger.error("Error in discontinuing", e);
    }
  }

  public boolean markAbortingState(@NotNull Interrupt interrupt, EnumSet<Status> statuses) {
    // Get all that are eligible for discontinuing
    List<NodeExecution> allNodeExecutions =
        nodeExecutionService.fetchNodeExecutionsByStatuses(interrupt.getPlanExecutionId(), statuses);
    if (isEmpty(allNodeExecutions)) {
      logger.warn(
          "No Node Executions could be marked as DISCONTINUING - planExecutionId: {}", interrupt.getPlanExecutionId());
      return false;
    }
    List<String> leafInstanceIds = getAllLeafInstanceIds(interrupt, allNodeExecutions, statuses);
    return nodeExecutionService.markLeavesDiscontinuingOnAbort(
        interrupt.getUuid(), interrupt.getType(), interrupt.getPlanExecutionId(), leafInstanceIds);
  }

  private List<String> getAllLeafInstanceIds(
      Interrupt interrupt, List<NodeExecution> allNodeExecutions, EnumSet<Status> statuses) {
    List<String> allInstanceIds = allNodeExecutions.stream().map(NodeExecution::getUuid).collect(toList());
    // Get Parent Ids
    List<String> parentIds = allNodeExecutions.stream()
                                 .filter(NodeExecution::isChildSpawningMode)
                                 .map(NodeExecution::getUuid)
                                 .collect(toList());
    if (isEmpty(parentIds)) {
      return allInstanceIds;
    }

    List<NodeExecution> children =
        nodeExecutionService.fetchChildrenNodeExecutionsByStatuses(interrupt.getPlanExecutionId(), parentIds, statuses);

    // get distinct parent Ids
    List<String> parentIdsHavingChildren =
        children.stream().map(NodeExecution::getParentId).distinct().collect(toList());

    // parent with no children
    allInstanceIds.removeAll(parentIdsHavingChildren);

    // Mark aborting
    return allInstanceIds;
  }
}
