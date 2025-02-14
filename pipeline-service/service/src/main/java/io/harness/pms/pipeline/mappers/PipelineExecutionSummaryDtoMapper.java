/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.StagesExecutionMetadata;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionIdentifierSummaryDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.pms.stages.BasicStageInfo;
import io.harness.pms.stages.StageExecutionSelectorHelper;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.utils.ExecutionModeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class PipelineExecutionSummaryDtoMapper {
  public PipelineExecutionSummaryDTO toDto(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity, EntityGitDetails entityGitDetails) {
    entityGitDetails = updateEntityGitDetails(entityGitDetails);
    Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap = pipelineExecutionSummaryEntity.getLayoutNodeMap();
    String startingNodeId = pipelineExecutionSummaryEntity.getStartingNodeId();
    StagesExecutionMetadata stagesExecutionMetadata = pipelineExecutionSummaryEntity.getStagesExecutionMetadata();
    boolean isStagesExecution = stagesExecutionMetadata != null && stagesExecutionMetadata.isStagesExecution();
    List<String> stageIdentifiers = stagesExecutionMetadata == null
            || ExecutionModeUtils.isRollbackMode(pipelineExecutionSummaryEntity.getExecutionMode())
        ? null
        : stagesExecutionMetadata.getStageIdentifiers();
    Map<String, String> stagesExecutedNames = null;
    if (EmptyPredicate.isNotEmpty(stageIdentifiers)) {
      stagesExecutedNames = getStageNames(stageIdentifiers, stagesExecutionMetadata.getFullPipelineYaml(),
          pipelineExecutionSummaryEntity.getPipelineVersion());
    }
    return PipelineExecutionSummaryDTO.builder()
        .name(pipelineExecutionSummaryEntity.getName())
        .orgIdentifier(pipelineExecutionSummaryEntity.getOrgIdentifier())
        .projectIdentifier(pipelineExecutionSummaryEntity.getProjectIdentifier())
        .createdAt(pipelineExecutionSummaryEntity.getCreatedAt())
        .layoutNodeMap(layoutNodeDTOMap)
        .moduleInfo(ModuleInfoMapper.getModuleInfo(pipelineExecutionSummaryEntity.getModuleInfo()))
        .startingNodeId(startingNodeId)
        .planExecutionId(pipelineExecutionSummaryEntity.getPlanExecutionId())
        .pipelineIdentifier(pipelineExecutionSummaryEntity.getPipelineIdentifier())
        .startTs(pipelineExecutionSummaryEntity.getStartTs())
        .endTs(pipelineExecutionSummaryEntity.getEndTs())
        .status(pipelineExecutionSummaryEntity.getStatus())
        .executionInputConfigured(pipelineExecutionSummaryEntity.getExecutionInputConfigured())
        .executionTriggerInfo(pipelineExecutionSummaryEntity.getExecutionTriggerInfo())
        .executionErrorInfo(pipelineExecutionSummaryEntity.getExecutionErrorInfo())
        .successfulStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId, ExecutionStatus.SUCCESS))
        .failedStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId, ExecutionStatus.FAILED))
        .runningStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId, ExecutionStatus.RUNNING))
        .totalStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId))
        .runSequence(pipelineExecutionSummaryEntity.getRunSequence())
        .tags(pipelineExecutionSummaryEntity.getTags())
        .labels(pipelineExecutionSummaryEntity.getLabels())
        .modules(EmptyPredicate.isEmpty(pipelineExecutionSummaryEntity.getModules())
                ? new ArrayList<>()
                : pipelineExecutionSummaryEntity.getModules())
        .gitDetails(entityGitDetails)
        .canRetry(!ExecutionModeUtils.isRollbackMode(pipelineExecutionSummaryEntity.getExecutionMode())
            && pipelineExecutionSummaryEntity.isLatestExecution())
        .showRetryHistory(!pipelineExecutionSummaryEntity.isLatestExecution()
            || !pipelineExecutionSummaryEntity.getPlanExecutionId().equals(
                pipelineExecutionSummaryEntity.getRetryExecutionMetadata().getRootExecutionId()))
        .governanceMetadata(pipelineExecutionSummaryEntity.getGovernanceMetadata())
        .isStagesExecution(isStagesExecution)
        .stagesExecuted(stageIdentifiers)
        .stagesExecutedNames(stagesExecutedNames)
        .parentStageInfo(pipelineExecutionSummaryEntity.getParentStageInfo())
        .allowStageExecutions(pipelineExecutionSummaryEntity.isStagesExecutionAllowed())
        .storeType(pipelineExecutionSummaryEntity.getStoreType())
        .connectorRef(EmptyPredicate.isEmpty(pipelineExecutionSummaryEntity.getConnectorRef())
                ? null
                : pipelineExecutionSummaryEntity.getConnectorRef())
        .abortedBy(pipelineExecutionSummaryEntity.getAbortedBy())
        .executionMode(pipelineExecutionSummaryEntity.getExecutionMode())
        .notesExistForPlanExecutionId(checkNotesExistForPlanExecutionId(pipelineExecutionSummaryEntity))
        .yamlVersion(pipelineExecutionSummaryEntity.getPipelineVersion())
        .shouldUseSimplifiedKey(checkShouldUseSimplifiedLogBaseKey(pipelineExecutionSummaryEntity))
        .build();
  }

  public boolean checkNotesExistForPlanExecutionId(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (null != pipelineExecutionSummaryEntity.getNotesExistForPlanExecutionId()) {
      return pipelineExecutionSummaryEntity.getNotesExistForPlanExecutionId();
    }
    return false;
  }

  public boolean checkShouldUseSimplifiedLogBaseKey(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (null != pipelineExecutionSummaryEntity.getShouldUseSimplifiedLogBaseKey()) {
      return pipelineExecutionSummaryEntity.getShouldUseSimplifiedLogBaseKey();
    }
    return false;
  }

  public PipelineExecutionIdentifierSummaryDTO toExecutionIdentifierDto(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    return PipelineExecutionIdentifierSummaryDTO.builder()
        .orgIdentifier(pipelineExecutionSummaryEntity.getOrgIdentifier())
        .projectIdentifier(pipelineExecutionSummaryEntity.getProjectIdentifier())
        .planExecutionId(pipelineExecutionSummaryEntity.getPlanExecutionId())
        .pipelineIdentifier(pipelineExecutionSummaryEntity.getPipelineIdentifier())
        .runSequence(pipelineExecutionSummaryEntity.getRunSequence())
        .status(pipelineExecutionSummaryEntity.getStatus())
        .build();
  }

  private Map<String, String> getStageNames(
      List<String> stageIdentifiers, String pipelineYaml, String pipelineVersion) {
    Map<String, String> identifierToNames = new LinkedHashMap<>();
    List<BasicStageInfo> stageInfoList;
    if (HarnessYamlVersion.V0.equals(pipelineVersion)) {
      { stageInfoList = StageExecutionSelectorHelper.getStageInfoList(pipelineYaml); }
    } else {
      stageInfoList = StageExecutionSelectorHelper.getStageInfoListV1(pipelineYaml);
    }
    stageInfoList.forEach(stageInfo -> {
      String identifier = stageInfo.getIdentifier();
      if (stageIdentifiers.contains(identifier)) {
        identifierToNames.put(identifier, stageInfo.getName());
      }
    });
    return identifierToNames;
  }

  public int getStagesCount(
      Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap, String startingNodeId, ExecutionStatus executionStatus) {
    if (startingNodeId == null) {
      return 0;
    }
    int count = 0;
    GraphLayoutNodeDTO nodeDTO = layoutNodeDTOMap.get(startingNodeId);
    if (!nodeDTO.getNodeType().equals("parallel") && nodeDTO.getStatus().equals(executionStatus)) {
      count++;
    } else if (nodeDTO.getNodeType().equals("parallel")) {
      for (String child : nodeDTO.getEdgeLayoutList().getCurrentNodeChildren()) {
        if (layoutNodeDTOMap.get(child).getStatus().equals(executionStatus)) {
          count++;
        }
      }
    }
    if (nodeDTO.getEdgeLayoutList().getNextIds().isEmpty()) {
      return count;
    }
    return count + getStagesCount(layoutNodeDTOMap, nodeDTO.getEdgeLayoutList().getNextIds().get(0), executionStatus);
  }
  public int getStagesCount(Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap, String startingNodeId) {
    if (startingNodeId == null) {
      return 0;
    }
    int count = 0;
    GraphLayoutNodeDTO nodeDTO = layoutNodeDTOMap.get(startingNodeId);
    if (!nodeDTO.getNodeType().equals("parallel")) {
      count++;
    } else if (nodeDTO.getNodeType().equals("parallel")) {
      count += nodeDTO.getEdgeLayoutList().getCurrentNodeChildren().size();
    }
    if (nodeDTO.getEdgeLayoutList().getNextIds().isEmpty()) {
      return count;
    }
    return count + getStagesCount(layoutNodeDTOMap, nodeDTO.getEdgeLayoutList().getNextIds().get(0));
  }

  private EntityGitDetails updateEntityGitDetails(EntityGitDetails entityGitDetails) {
    if (entityGitDetails == null) {
      return null;
    }
    String rootFolder = entityGitDetails.getRootFolder();
    String filePath = entityGitDetails.getFilePath();
    String repoIdentifier = entityGitDetails.getRepoIdentifier();
    String repoName = entityGitDetails.getRepoName();
    String branch = entityGitDetails.getBranch();
    String objectId = entityGitDetails.getObjectId();
    String commitId = entityGitDetails.getCommitId();
    return EntityGitDetails.builder()
        .rootFolder(EntityGitDetailsMapper.nullIfDefault(rootFolder))
        .filePath(EntityGitDetailsMapper.nullIfDefault(filePath))
        .repoIdentifier(EntityGitDetailsMapper.nullIfDefault(repoIdentifier))
        .repoName(EntityGitDetailsMapper.nullIfDefault(repoName))
        .branch(EntityGitDetailsMapper.nullIfDefault(branch))
        .objectId(EntityGitDetailsMapper.nullIfDefault(objectId))
        .commitId(EntityGitDetailsMapper.nullIfDefault(commitId))
        .build();
  }
}
