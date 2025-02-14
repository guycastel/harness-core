/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.states.codebase;

import static io.harness.beans.execution.ExecutionSource.Type.MANUAL;
import static io.harness.beans.execution.ExecutionSource.Type.WEBHOOK;
import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODEBASE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;

import static software.wings.beans.TaskType.SCM_GIT_REF_TASK;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.app.beans.entities.StepExecutionParameters;
import io.harness.beans.FeatureName;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.CommitDetails;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.ReleaseWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.sweepingoutputs.Build;
import io.harness.beans.sweepingoutputs.CodebaseSweepingOutput;
import io.harness.beans.sweepingoutputs.CodebaseSweepingOutput.CodeBaseCommit;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.StageDetails;
import io.harness.ci.execution.buildstate.CodebaseUtils;
import io.harness.ci.execution.buildstate.ConnectorUtils;
import io.harness.ci.execution.utils.WebhookTriggerProcessorUtils;
import io.harness.ci.ff.CIFeatureFlagService;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.GitRefType;
import io.harness.delegate.task.scm.ScmGitRefTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.FindPRResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.repositories.StepExecutionParametersRepository;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.TaskRequestsUtils;
import io.harness.supplier.ThrowingSupplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CodeBaseTaskStep implements TaskExecutable<CodeBaseTaskStepParameters, ScmGitRefTaskResponseData>,
                                         SyncExecutable<CodeBaseTaskStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("CI_CODEBASE_TASK").setStepCategory(StepCategory.STEP).build();
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;

  @Inject private ConnectorUtils connectorUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Inject private ScmGitRefManager scmGitRefManager;
  @Inject private CIFeatureFlagService featureFlagService;
  @Inject private StepExecutionParametersRepository stepExecutionParametersRepository;

  @Override
  public Class<CodeBaseTaskStepParameters> getStepParametersClass() {
    return CodeBaseTaskStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, CodeBaseTaskStepParameters stepParameters, StepInputPackage inputPackage) {
    ExecutionSource executionSource = getExecutionSource(ambiance, stepParameters.getExecutionSource());
    if (executionSource.getType() != MANUAL) {
      throw new CIStageExecutionException("{} type is not supported in codebase delegate task for scm api operation");
    }

    String runTime = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String stageRuntimeId = AmbianceUtils.getStageRuntimeIdAmbiance(ambiance);

    stepExecutionParametersRepository.save(StepExecutionParameters.builder()
                                               .accountId(accountId)
                                               .runTimeId(runTime)
                                               .stageRunTimeId(stageRuntimeId)
                                               .stepParameters(RecastOrchestrationUtils.toJson(stepParameters))
                                               .build());

    ManualExecutionSource manualExecutionSource = (ManualExecutionSource) executionSource;
    ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(AmbianceUtils.getNgAccess(ambiance),
        RunTimeInputHandler.resolveStringParameterV2("connectorRef", STEP_TYPE.getType(),
            ambiance.getStageExecutionId(), stepParameters.getConnectorRef(), false),
        true);

    ScmGitRefTaskParams scmGitRefTaskParams = obtainTaskParameters(manualExecutionSource, connectorDetails,
        RunTimeInputHandler.resolveStringParameterV2(
            "repoName", STEP_TYPE.getType(), ambiance.getStageExecutionId(), stepParameters.getRepoName(), false));

    List<TaskSelector> selectors = new ArrayList<>();
    if (featureFlagService.isEnabled(FeatureName.CI_CODEBASE_SELECTOR, accountId)) {
      selectors =
          connectorUtils.fetchCodebaseDelegateSelector(ambiance, connectorDetails, executionSweepingOutputResolver);
    }

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(Duration.ofSeconds(30).toMillis())
                                  .taskType(SCM_GIT_REF_TASK.name())
                                  .parameters(new Object[] {scmGitRefTaskParams})
                                  .build();

    log.info("Created delegate task to fetch codebase info");
    return TaskRequestsUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, referenceFalseKryoSerializer,
        TaskCategory.DELEGATE_TASK_V2, Collections.emptyList(), true, null, selectors);
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, CodeBaseTaskStepParameters stepParameters,
      ThrowingSupplier<ScmGitRefTaskResponseData> responseDataSupplier) throws Exception {
    ScmGitRefTaskResponseData scmGitRefTaskResponseData = null;
    try {
      log.info("Retrieving codebase info from returned delegate response");
      scmGitRefTaskResponseData = responseDataSupplier.get();
      log.info("Successfully retrieved codebase info from returned delegate response");
    } catch (Exception ex) {
      ManualExecutionSource manualExecutionSource =
          (ManualExecutionSource) getExecutionSource(ambiance, stepParameters.getExecutionSource());
      String prNumber = manualExecutionSource.getPrNumber();
      if (scmGitRefTaskResponseData == null && isNotEmpty(prNumber)) {
        log.error("Failed to retrieve codebase info from returned delegate response with PR number: " + prNumber, ex);
        throw new CIStageExecutionException("Failed to retrieve PrNumber: " + prNumber + " details");
      }
      log.error("Failed to retrieve codebase info from returned delegate response", ex);
    }

    saveScmResponseToSweepingOutput(ambiance, stepParameters, scmGitRefTaskResponseData);
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, CodeBaseTaskStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    ExecutionSource executionSource = getExecutionSource(ambiance, stepParameters.getExecutionSource());

    String runTime = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String stageRuntimeId = AmbianceUtils.getStageRuntimeIdAmbiance(ambiance);

    stepExecutionParametersRepository.save(StepExecutionParameters.builder()
                                               .accountId(accountId)
                                               .runTimeId(runTime)
                                               .stageRunTimeId(stageRuntimeId)
                                               .stepParameters(RecastOrchestrationUtils.toJson(stepParameters))
                                               .build());

    CodebaseSweepingOutput codebaseSweepingOutput = null;
    String connectorRef = RunTimeInputHandler.resolveStringParameterV2(
        "connectorRef", STEP_TYPE.getType(), ambiance.getStageExecutionId(), stepParameters.getConnectorRef(), false);
    String repoName = RunTimeInputHandler.resolveStringParameterV2(
        "repoName", STEP_TYPE.getType(), ambiance.getStageExecutionId(), stepParameters.getRepoName(), false);
    if (executionSource.getType() == MANUAL) {
      NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
      ConnectorDetails connectorDetails =
          connectorUtils.getConnectorDetailsWithToken(ngAccess, connectorRef, true, ambiance, repoName);
      ManualExecutionSource manualExecutionSource = (ManualExecutionSource) executionSource;
      // fetch scm details via manager
      if (connectorUtils.hasApiAccess(connectorDetails)) {
        String branch = manualExecutionSource.getBranch();
        String prNumber = manualExecutionSource.getPrNumber();
        String tag = manualExecutionSource.getTag();
        try {
          ScmConnector scmConnector =
              scmGitRefManager.getScmConnector(connectorDetails, ngAccess.getAccountIdentifier(), repoName);
          ScmGitRefTaskResponseData response = scmGitRefManager.fetchCodebaseMetadata(
              scmConnector, connectorDetails.getIdentifier(), branch, prNumber, tag);
          saveScmResponseToSweepingOutput(ambiance, stepParameters, response);
          return StepResponse.builder().status(Status.SUCCEEDED).build();
        } catch (Exception ex) {
          log.error("Failed to fetch codebase metadata", ex);
          return StepResponse.builder()
              .status(Status.FAILED)
              .failureInfo(FailureInfo.newBuilder().setErrorMessage(emptyIfNull(ex.getMessage())).build())
              .build();
        }
      } else {
        if (isNotEmpty(manualExecutionSource.getPrNumber())) {
          throw new CIStageExecutionException(
              "PR build type is not supported when api access is disabled in git connector or clone codebase is false");
        }
        String repoUrl = CodebaseUtils.getCompleteURLFromConnector(connectorDetails, repoName);
        codebaseSweepingOutput = buildManualCodebaseSweepingOutput(manualExecutionSource, repoUrl);
      }
    } else if (executionSource.getType() == WEBHOOK) {
      codebaseSweepingOutput = buildWebhookCodebaseSweepingOutput((WebhookExecutionSource) executionSource);
    }
    saveCodebaseSweepingOutput(ambiance, codebaseSweepingOutput);

    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private void saveScmResponseToSweepingOutput(Ambiance ambiance, CodeBaseTaskStepParameters stepParameters,
      ScmGitRefTaskResponseData scmGitRefTaskResponseData) throws InvalidProtocolBufferException {
    CodebaseSweepingOutput codebaseSweepingOutput = null;
    if (scmGitRefTaskResponseData != null
        && scmGitRefTaskResponseData.getGitRefType() == GitRefType.PULL_REQUEST_WITH_COMMITS) {
      codebaseSweepingOutput = buildPRCodebaseSweepingOutput(scmGitRefTaskResponseData);
    } else if (scmGitRefTaskResponseData != null
        && scmGitRefTaskResponseData.getGitRefType() == GitRefType.LATEST_COMMIT_ID) {
      ManualExecutionSource manualExecutionSource =
          (ManualExecutionSource) getExecutionSource(ambiance, stepParameters.getExecutionSource());
      codebaseSweepingOutput =
          buildCommitShaCodebaseSweepingOutput(scmGitRefTaskResponseData, manualExecutionSource.getTag());
    }
    if (codebaseSweepingOutput != null) {
      saveCodebaseSweepingOutput(ambiance, codebaseSweepingOutput);
    }
  }

  @VisibleForTesting
  ScmGitRefTaskParams obtainTaskParameters(
      ManualExecutionSource manualExecutionSource, ConnectorDetails connectorDetails, String repoName) {
    ScmConnector scmConnector = (ScmConnector) connectorDetails.getConnectorConfig();
    String completeUrl = CodebaseUtils.getCompleteURLFromConnector(connectorDetails, repoName);
    scmConnector.setUrl(completeUrl);

    String branch = manualExecutionSource.getBranch();
    String prNumber = manualExecutionSource.getPrNumber();
    String tag = manualExecutionSource.getTag();
    if (isNotEmpty(branch)) {
      return ScmGitRefTaskParams.builder()
          .branch(branch)
          .gitRefType(GitRefType.LATEST_COMMIT_ID)
          .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
          .scmConnector(scmConnector)
          .build();
    } else if (isNotEmpty(prNumber)) {
      return ScmGitRefTaskParams.builder()
          .prNumber(Long.parseLong(prNumber))
          .gitRefType(GitRefType.PULL_REQUEST_WITH_COMMITS)
          .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
          .scmConnector((ScmConnector) connectorDetails.getConnectorConfig())
          .build();
    } else if (isNotEmpty(tag)) {
      return ScmGitRefTaskParams.builder()
          .ref(tag)
          .gitRefType(GitRefType.LATEST_COMMIT_ID)
          .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
          .scmConnector(scmConnector)
          .build();
    } else {
      throw new CIStageExecutionException("Manual codebase git task needs one of PR number, branch or tag");
    }
  }

  @VisibleForTesting
  CodebaseSweepingOutput buildCommitShaCodebaseSweepingOutput(
      ScmGitRefTaskResponseData scmGitRefTaskResponseData, String tag) throws InvalidProtocolBufferException {
    final byte[] getLatestCommitResponseByteArray = scmGitRefTaskResponseData.getGetLatestCommitResponse();
    if (isEmpty(getLatestCommitResponseByteArray)) {
      throw new CIStageExecutionException("Codebase git commit information can't be obtained");
    }
    GetLatestCommitResponse latestCommitResponse = GetLatestCommitResponse.parseFrom(getLatestCommitResponseByteArray);

    if (latestCommitResponse.getCommit() == null || isEmpty(latestCommitResponse.getCommit().getSha())) {
      return null;
    }

    String branch = getPropertyFromCommitOrDefault(scmGitRefTaskResponseData.getBranch(), "");
    String ref = String.format("refs/heads/%s", branch);

    Build build = new Build("branch");
    if (isNotEmpty(tag)) {
      build = new Build("tag");
      ref = String.format("refs/tags/%s", tag);
    }

    String commitSha = latestCommitResponse.getCommit().getSha();
    String shortCommitSha = WebhookTriggerProcessorUtils.getShortCommitSha(commitSha);

    return CodebaseSweepingOutput.builder()
        .branch(branch)
        .sourceBranch(branch)
        .targetBranch(branch)
        .tag(tag)
        .build(build)
        .commits(asList(CodeBaseCommit.builder()
                            .id(latestCommitResponse.getCommit().getSha())
                            .link(latestCommitResponse.getCommit().getLink())
                            .message(latestCommitResponse.getCommit().getMessage())
                            .ownerEmail(latestCommitResponse.getCommit().getAuthor().getEmail())
                            .ownerName(latestCommitResponse.getCommit().getAuthor().getName())
                            .ownerId(latestCommitResponse.getCommit().getAuthor().getLogin())
                            .timeStamp(latestCommitResponse.getCommit().getAuthor().getDate().getSeconds())
                            .build()))
        .commitSha(commitSha)
        .shortCommitSha(shortCommitSha)
        .repoUrl(scmGitRefTaskResponseData.getRepoUrl())
        .gitUserId(latestCommitResponse.getCommit().getAuthor().getLogin())
        .gitUser(latestCommitResponse.getCommit().getAuthor().getName())
        .gitUserEmail(latestCommitResponse.getCommit().getAuthor().getEmail())
        .gitUserAvatar(latestCommitResponse.getCommit().getAuthor().getAvatar())
        .commitMessage(latestCommitResponse.getCommit().getMessage())
        .commitRef(ref)
        .build();
  }

  @VisibleForTesting
  CodebaseSweepingOutput buildWebhookCodebaseSweepingOutput(WebhookExecutionSource webhookExecutionSource) {
    List<CodebaseSweepingOutput.CodeBaseCommit> codeBaseCommits = new ArrayList<>();
    if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
      PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();

      if (isNotEmpty(prWebhookEvent.getCommitDetailsList())) {
        for (CommitDetails commit : prWebhookEvent.getCommitDetailsList()) {
          codeBaseCommits.add(CodebaseSweepingOutput.CodeBaseCommit.builder()
                                  .id(commit.getCommitId())
                                  .message(commit.getMessage())
                                  .link(commit.getLink())
                                  .timeStamp(commit.getTimeStamp())
                                  .ownerEmail(commit.getOwnerEmail())
                                  .ownerId(commit.getOwnerId())
                                  .ownerName(commit.getOwnerName())
                                  .build());
        }
      }
      sortCommitsInDecreasingTimeStamp(codeBaseCommits);

      String commitSha = prWebhookEvent.getBaseAttributes().getAfter();
      String shortCommitSha = WebhookTriggerProcessorUtils.getShortCommitSha(commitSha);
      String mergeCommitSha = prWebhookEvent.getBaseAttributes().getMergeSha();
      String commitRef = prWebhookEvent.getBaseAttributes().getRef();
      CodeBaseCommit codeBaseCommit =
          isNotEmpty(codeBaseCommits) ? codeBaseCommits.get(0) : CodeBaseCommit.builder().build();

      return CodebaseSweepingOutput.builder()
          .commits(codeBaseCommits)
          .state(getState(prWebhookEvent))
          .branch(prWebhookEvent.getTargetBranch())
          .targetBranch(prWebhookEvent.getTargetBranch())
          .sourceBranch(prWebhookEvent.getSourceBranch())
          .prNumber(String.valueOf(prWebhookEvent.getPullRequestId()))
          .prTitle(prWebhookEvent.getTitle())
          .build(new Build("PR"))
          .commitSha(commitSha)
          .mergeSha(mergeCommitSha)
          .shortCommitSha(shortCommitSha)
          .baseCommitSha(prWebhookEvent.getBaseAttributes().getBefore())
          .repoUrl(prWebhookEvent.getRepository().getLink())
          .pullRequestLink(prWebhookEvent.getPullRequestLink())
          .gitUser(getPropertyFromCommitOrDefault(
              prWebhookEvent.getBaseAttributes().getAuthorName(), codeBaseCommit.getOwnerName()))
          .gitUserEmail(getPropertyFromCommitOrDefault(
              prWebhookEvent.getBaseAttributes().getAuthorEmail(), codeBaseCommit.getOwnerEmail()))
          .gitUserAvatar(prWebhookEvent.getBaseAttributes().getAuthorAvatar())
          .gitUserId(getPropertyFromCommitOrDefault(
              prWebhookEvent.getBaseAttributes().getAuthorLogin(), codeBaseCommit.getOwnerId()))
          .commitMessage(getCommitMessage(codeBaseCommits))
          .commitRef(commitRef)
          .build();
    } else if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
      BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();

      if (isNotEmpty(branchWebhookEvent.getCommitDetailsList())) {
        for (CommitDetails commit : branchWebhookEvent.getCommitDetailsList()) {
          codeBaseCommits.add(CodebaseSweepingOutput.CodeBaseCommit.builder()
                                  .id(commit.getCommitId())
                                  .message(commit.getMessage())
                                  .link(commit.getLink())
                                  .timeStamp(commit.getTimeStamp())
                                  .ownerEmail(commit.getOwnerEmail())
                                  .ownerId(commit.getOwnerId())
                                  .ownerName(commit.getOwnerName())
                                  .build());
        }
      }
      sortCommitsInDecreasingTimeStamp(codeBaseCommits);

      String commitSha = branchWebhookEvent.getBaseAttributes().getAfter();
      String shortCommitSha = WebhookTriggerProcessorUtils.getShortCommitSha(commitSha);
      String branch = getPropertyFromCommitOrDefault(branchWebhookEvent.getBranchName(), "");

      return CodebaseSweepingOutput.builder()
          .branch(branch)
          .commits(codeBaseCommits)
          .build(new Build("branch"))
          .sourceBranch(branch)
          .targetBranch(branch)
          .commitSha(commitSha)
          .shortCommitSha(shortCommitSha)
          .repoUrl(branchWebhookEvent.getRepository().getLink())
          .gitUser(branchWebhookEvent.getBaseAttributes().getAuthorName())
          .gitUserEmail(branchWebhookEvent.getBaseAttributes().getAuthorEmail())
          .gitUserAvatar(branchWebhookEvent.getBaseAttributes().getAuthorAvatar())
          .gitUserId(branchWebhookEvent.getBaseAttributes().getAuthorLogin())
          .commitMessage(getCommitMessage(codeBaseCommits))
          .commitRef(branchWebhookEvent.getBaseAttributes().getRef())
          .build();
    } else if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.RELEASE) {
      ReleaseWebhookEvent releaseWebhookEvent = (ReleaseWebhookEvent) webhookExecutionSource.getWebhookEvent();

      return CodebaseSweepingOutput.builder()
          .build(new Build("release"))
          .releaseTag(releaseWebhookEvent.getReleaseTag())
          .releaseLink(releaseWebhookEvent.getReleaseLink())
          .releaseTitle(releaseWebhookEvent.getTitle())
          .releaseBody(releaseWebhookEvent.getReleaseBody())
          .repoUrl(releaseWebhookEvent.getRepository().getLink())
          .gitUser(releaseWebhookEvent.getBaseAttributes().getAuthorName())
          .gitUserEmail(releaseWebhookEvent.getBaseAttributes().getAuthorEmail())
          .gitUserAvatar(releaseWebhookEvent.getBaseAttributes().getAuthorAvatar())
          .gitUserId(releaseWebhookEvent.getBaseAttributes().getAuthorLogin())
          .build();
    }
    return CodebaseSweepingOutput.builder().build();
  }

  @VisibleForTesting
  CodebaseSweepingOutput buildManualCodebaseSweepingOutput(
      ManualExecutionSource manualExecutionSource, String repoUrl) {
    Build build = new Build("branch");
    if (isNotEmpty(manualExecutionSource.getTag())) {
      build = new Build("tag");
    }

    String commitSha = manualExecutionSource.getCommitSha();
    String shortCommitSha = WebhookTriggerProcessorUtils.getShortCommitSha(commitSha);
    String branch = getPropertyFromCommitOrDefault(manualExecutionSource.getBranch(), "");

    return CodebaseSweepingOutput.builder()
        .build(build)
        .branch(branch)
        .sourceBranch(branch)
        .targetBranch(branch)
        .tag(manualExecutionSource.getTag())
        .commitSha(commitSha)
        .shortCommitSha(shortCommitSha)
        .repoUrl(repoUrl)
        .build();
  }

  @VisibleForTesting
  public CodebaseSweepingOutput buildPRCodebaseSweepingOutput(ScmGitRefTaskResponseData scmGitRefTaskResponseData)
      throws InvalidProtocolBufferException {
    CodebaseSweepingOutput codebaseSweepingOutput;
    final byte[] findPRResponseByteArray = scmGitRefTaskResponseData.getFindPRResponse();
    final byte[] listCommitsInPRResponseByteArray = scmGitRefTaskResponseData.getListCommitsInPRResponse();
    final String repoUrl = scmGitRefTaskResponseData.getRepoUrl();

    if (findPRResponseByteArray == null || listCommitsInPRResponseByteArray == null) {
      throw new CIStageExecutionException("Codebase git information can't be obtained");
    }

    FindPRResponse findPRResponse = FindPRResponse.parseFrom(findPRResponseByteArray);
    ListCommitsInPRResponse listCommitsInPRResponse =
        ListCommitsInPRResponse.parseFrom(listCommitsInPRResponseByteArray);
    PullRequest pr = findPRResponse.getPr();

    List<Commit> commits = listCommitsInPRResponse.getCommitsList();
    List<CodebaseSweepingOutput.CodeBaseCommit> codeBaseCommits = new ArrayList<>();
    for (Commit commit : commits) {
      codeBaseCommits.add(CodebaseSweepingOutput.CodeBaseCommit.builder()
                              .id(commit.getSha())
                              .message(commit.getMessage())
                              .link(commit.getLink())
                              .timeStamp(commit.getCommitter().getDate().getSeconds())
                              .ownerEmail(commit.getAuthor().getEmail())
                              .ownerId(commit.getAuthor().getLogin())
                              .ownerName(commit.getAuthor().getName())
                              .build());
    }
    sortCommitsInDecreasingTimeStamp(codeBaseCommits);

    String commitSha = pr.getSha();
    String shortCommitSha = WebhookTriggerProcessorUtils.getShortCommitSha(commitSha);
    CodeBaseCommit codeBaseCommit =
        isNotEmpty(codeBaseCommits) ? codeBaseCommits.get(0) : CodeBaseCommit.builder().build();

    codebaseSweepingOutput =
        CodebaseSweepingOutput.builder()
            .branch(pr.getTarget())
            .sourceBranch(pr.getSource())
            .targetBranch(pr.getTarget())
            .prNumber(String.valueOf(pr.getNumber()))
            .prTitle(pr.getTitle())
            .commitSha(commitSha)
            .mergeSha(pr.getMergeSha())
            .shortCommitSha(shortCommitSha)
            .build(new Build("PR"))
            .baseCommitSha(pr.getBase().getSha())
            .commitRef(pr.getRef())
            .repoUrl(repoUrl) // Add repo url to scm.PullRequest and get it from there
            .gitUser(getPropertyFromCommitOrDefault(pr.getAuthor().getName(), codeBaseCommit.getOwnerName()))
            .gitUserAvatar(pr.getAuthor().getAvatar())
            .gitUserEmail(getPropertyFromCommitOrDefault(pr.getAuthor().getEmail(), codeBaseCommit.getOwnerEmail()))
            .gitUserId(getPropertyFromCommitOrDefault(pr.getAuthor().getLogin(), codeBaseCommit.getOwnerId()))
            .pullRequestLink(pr.getLink())
            .commits(codeBaseCommits)
            .state(getState(pr))
            .commitMessage(getCommitMessage(codeBaseCommits))
            .build();
    return codebaseSweepingOutput;
  }

  private void saveCodebaseSweepingOutput(Ambiance ambiance, CodebaseSweepingOutput codebaseSweepingOutput) {
    OptionalSweepingOutput optionalSweepingOutput =
        executionSweepingOutputResolver.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(CODEBASE));
    if (!optionalSweepingOutput.isFound()) {
      try {
        executionSweepingOutputResolver.consume(
            ambiance, CODEBASE, codebaseSweepingOutput, StepOutcomeGroup.PIPELINE.name());
      } catch (Exception e) {
        log.error("Error while consuming codebase sweeping output", e);
      }
    }
  }

  @NotNull
  private String getState(PullRequest pr) {
    String state = "open";
    if (pr.getMerged()) {
      state = "merged";
    } else if (pr.getClosed()) {
      state = "closed";
    }
    return state;
  }

  @NotNull
  private String getState(PRWebhookEvent pr) {
    String state = "open";
    if (pr.isMerged()) {
      state = "merged";
    } else if (pr.isClosed()) {
      state = "closed";
    }
    return state;
  }

  private String getCommitMessage(List<CodebaseSweepingOutput.CodeBaseCommit> commits) {
    if (!commits.isEmpty()) {
      return commits.get(0).getMessage();
    }
    return null;
  }

  private void sortCommitsInDecreasingTimeStamp(List<CodebaseSweepingOutput.CodeBaseCommit> commits) {
    Collections.sort(commits, (a, b) -> a.getTimeStamp() > b.getTimeStamp() ? -1 : 1);
  }

  private ExecutionSource getExecutionSource(Ambiance ambiance, ExecutionSource executionSource) {
    if (executionSource != null) {
      return executionSource;
    }
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputResolver.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.stageDetails));
    if (!optionalSweepingOutput.isFound()) {
      throw new CIStageExecutionException("Unable to fetch stage details. Please retry or verify pipeline yaml");
    }
    StageDetails stageDetails = (StageDetails) optionalSweepingOutput.getOutput();
    return stageDetails.getExecutionSource();
  }

  private String getPropertyFromCommitOrDefault(String value, String defaultValue) {
    if (isNotEmpty(value)) {
      return value;
    }
    return defaultValue;
  }
}
