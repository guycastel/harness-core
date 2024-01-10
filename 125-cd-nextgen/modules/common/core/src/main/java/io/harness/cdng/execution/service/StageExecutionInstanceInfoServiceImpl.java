/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.execution.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.execution.StageExecutionInstanceInfo;
import io.harness.delegate.cdng.execution.StepExecutionInstanceInfo;
import io.harness.repositories.executions.StageExecutionInstanceInfoRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(CDP)
public class StageExecutionInstanceInfoServiceImpl implements StageExecutionInstanceInfoService {
  private StageExecutionInstanceInfoRepository repository;
  @Override
  public List<StepExecutionInstanceInfo> get(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineExecutionId, String stageExecutionId) {
    Optional<StageExecutionInstanceInfo> instanceInfo =
        repository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndPipelineExecutionIdAndStageExecutionId(
            accountIdentifier, orgIdentifier, projectIdentifier, pipelineExecutionId, stageExecutionId);
    return instanceInfo.isPresent() ? instanceInfo.get().getInstanceInfos() : new ArrayList<>();
  }

  @Override
  public StageExecutionInstanceInfo append(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineExecutionId, String stageExecutionId, StepExecutionInstanceInfo stepExecutionInstanceInfo,
      String stepPath) {
    return repository.append(accountIdentifier, orgIdentifier, projectIdentifier, pipelineExecutionId, stageExecutionId,
        stepExecutionInstanceInfo, stepPath);
  }
}
