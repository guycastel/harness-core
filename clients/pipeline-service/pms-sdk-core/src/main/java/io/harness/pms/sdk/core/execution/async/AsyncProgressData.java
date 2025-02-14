/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.execution.async;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.Status;
import io.harness.tasks.ProgressData;

import lombok.Builder;
import lombok.Value;

/**
 * This progress callback updates the status for Task which is waiting
 */
@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class AsyncProgressData implements ProgressData {
  // New status of the node
  @Builder.Default Status status = Status.NO_OP;
}
