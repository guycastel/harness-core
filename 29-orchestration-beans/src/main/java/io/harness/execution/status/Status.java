package io.harness.execution.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;

@OwnedBy(CDC)
@Redesign
public enum Status {
  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,

  INTERVENTION_WAITING,
  TIMED_WAITING,
  ASYNC_WAITING,
  TASK_WAITING,

  DISCONTINUING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  SKIPPED,
  PAUSED,
  ABORTED,
  ERRORED,
  FAILED,
  EXPIRED,
  SUCCEEDED;

  // Status Groups
  private static final EnumSet<Status> FINALIZABLE_STATUSES =
      EnumSet.of(QUEUED, RUNNING, PAUSED, ASYNC_WAITING, TASK_WAITING, TIMED_WAITING, DISCONTINUING);

  private static final EnumSet<Status> POSITIVE_STATUSES = EnumSet.of(SUCCEEDED, SKIPPED);

  private static final EnumSet<Status> BROKE_STATUSES = EnumSet.of(FAILED, ERRORED);

  private static final EnumSet<Status> RESUMABLE_STATUSES =
      EnumSet.of(QUEUED, RUNNING, ASYNC_WAITING, TASK_WAITING, TIMED_WAITING, INTERVENTION_WAITING);

  private static final EnumSet<Status> FLOWING_STATUSES =
      EnumSet.of(RUNNING, ASYNC_WAITING, TASK_WAITING, TIMED_WAITING, DISCONTINUING);

  private static final EnumSet<Status> FINAL_STATUSES =
      EnumSet.of(QUEUED, SKIPPED, PAUSED, ABORTED, ERRORED, FAILED, EXPIRED, SUCCEEDED);

  private static final EnumSet<Status> RETRYABLE_STATUSES = EnumSet.of(FAILED, ERRORED, EXPIRED);

  public static EnumSet<Status> finalizableStatuses() {
    return FINALIZABLE_STATUSES;
  }

  public static EnumSet<Status> positiveStatuses() {
    return POSITIVE_STATUSES;
  }

  public static EnumSet<Status> brokeStatuses() {
    return BROKE_STATUSES;
  }

  public static EnumSet<Status> resumableStatuses() {
    return RESUMABLE_STATUSES;
  }

  public static EnumSet<Status> flowingStatuses() {
    return FLOWING_STATUSES;
  }

  public static EnumSet<Status> retryableStatuses() {
    return RETRYABLE_STATUSES;
  }

  public static EnumSet<Status> finalStatuses() {
    return FINAL_STATUSES;
  }

  public static EnumSet<Status> obtainAllowedStartSet(Status status) {
    switch (status) {
      case RUNNING:
        return EnumSet.of(QUEUED, ASYNC_WAITING, TASK_WAITING, TIMED_WAITING, INTERVENTION_WAITING, PAUSED);
      case INTERVENTION_WAITING:
        return BROKE_STATUSES;
      case TIMED_WAITING:
      case ASYNC_WAITING:
      case TASK_WAITING:
      case PAUSED:
        return EnumSet.of(QUEUED, RUNNING);
      case DISCONTINUING:
        return EnumSet.of(QUEUED, RUNNING, ASYNC_WAITING, TASK_WAITING, TIMED_WAITING, INTERVENTION_WAITING, PAUSED);
      case SKIPPED:
        return EnumSet.of(QUEUED);
      case QUEUED:
        return EnumSet.of(PAUSED);
      case ABORTED:
      case SUCCEEDED:
      case ERRORED:
      case FAILED:
        return FINALIZABLE_STATUSES;
      default:
        throw new IllegalStateException("Unexpected value: " + status);
    }
  }
}
