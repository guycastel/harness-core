/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.abort.AbortedBy;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.validator.Trimmed;
import io.harness.dto.FailureInfoDTO;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.governance.GovernanceMetadata;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.plan.ExecutionMode;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PipelineStageInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.yaml.core.NGLabel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(PIPELINE)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("PipelineExecutionSummary")
@Schema(name = "PipelineExecutionSummary", description = "This is the view of the Pipeline Execution Summary")
public class PipelineExecutionSummaryDTO {
  String pipelineIdentifier;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  String planExecutionId;
  String name;
  // Stores the pipeline yaml version
  String yamlVersion;

  ExecutionStatus status;

  List<NGTag> tags;
  List<NGLabel> labels;

  ExecutionTriggerInfo executionTriggerInfo;
  @Deprecated ExecutionErrorInfo executionErrorInfo;
  GovernanceMetadata governanceMetadata;
  FailureInfoDTO failureInfo;

  Map<String, LinkedHashMap<String, Object>> moduleInfo;
  Map<String, GraphLayoutNodeDTO> layoutNodeMap;
  List<String> modules;
  String startingNodeId;

  Long startTs;
  Long endTs;
  Long createdAt;

  boolean canRetry;
  boolean showRetryHistory;

  int runSequence;
  long successfulStagesCount;
  long runningStagesCount;
  long failedStagesCount;
  long totalStagesCount;
  EntityGitDetails gitDetails;
  StoreType storeType;
  String connectorRef;

  Boolean executionInputConfigured;
  boolean isStagesExecution;
  PipelineStageInfo parentStageInfo;
  List<String> stagesExecuted;
  Map<String, String> stagesExecutedNames;
  boolean allowStageExecutions;
  AbortedBy abortedBy;

  ExecutionMode executionMode;
  boolean notesExistForPlanExecutionId;
  boolean shouldUseSimplifiedKey;
}
