/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.expression.LateBindingMap;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class ExecutionSweepingOutputFunctor extends LateBindingMap {
  transient PmsSweepingOutputService pmsSweepingOutputService;
  transient Ambiance ambiance;

  @Override
  public synchronized Object get(Object key) {
    String json = pmsSweepingOutputService.resolve(ambiance, RefObjectUtils.getSweepingOutputRefObject((String) key));
    return json == null ? null : NodeExecutionUtils.extractAndProcessObject(json);
  }
}
