/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.git;

import static io.harness.git.Constants.GIT_YAML_LOG_PREFIX;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitCommandTaskHandler;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.connector.task.git.GitHubAppAuthenticationHelper;
import io.harness.connector.task.git.ScmConnectorMapperDelegate;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.common.AbstractDelegateRunnableTask;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.ListRemoteRequest;
import io.harness.git.model.ListRemoteResult;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class NGGitCommandTask extends AbstractDelegateRunnableTask {
  @Inject private NGGitService gitService;
  @Inject private GitCommandTaskHandler gitCommandTaskHandler;
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private GitHubAppAuthenticationHelper gitHubAppAuthenticationHelper;
  @Inject private ScmConnectorMapperDelegate scmConnectorMapperDelegate;

  public NGGitCommandTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    GitCommandParams gitCommandParams = (GitCommandParams) parameters;
    GitConfigDTO gitConfig = scmConnectorMapperDelegate.toGitConfigDTO(
        gitCommandParams.getScmConnector(), gitCommandParams.getEncryptionDetails());
    gitDecryptionHelper.decryptGitConfig(gitConfig, gitCommandParams.getEncryptionDetails());
    SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(
        gitCommandParams.getSshKeySpecDTO(), gitCommandParams.getEncryptionDetails());
    GitCommandType gitCommandType = gitCommandParams.getGitCommandType();
    GitBaseRequest gitCommandRequest = gitCommandParams.getGitCommandRequest();
    ScmConnector scmConnector = gitCommandParams.getScmConnector();
    gitDecryptionHelper.decryptApiAccessConfig(scmConnector, gitCommandParams.getEncryptionDetails());

    switch (gitCommandType) {
      case VALIDATE:
        GitCommandExecutionResponse delegateResponseData;
        if (gitCommandParams.isGithubAppAuthentication()) {
          delegateResponseData = (GitCommandExecutionResponse) gitCommandTaskHandler.handleValidateTaskForGithubAppAuth(
              (GithubConnectorDTO) scmConnector, gitCommandParams.getEncryptionDetails());
        } else {
          delegateResponseData = (GitCommandExecutionResponse) gitCommandTaskHandler.handleValidateTask(
              gitConfig, scmConnector, getAccountId(), sshSessionConfig);
        }
        delegateResponseData.setDelegateMetaInfo(DelegateMetaInfo.builder().id(getDelegateId()).build());
        return delegateResponseData;
      case LIST_REMOTE:
        return handleListRemote(gitCommandParams, gitConfig, sshSessionConfig);
      case COMMIT_AND_PUSH:
        return handleCommitAndPush(gitCommandParams, gitConfig, sshSessionConfig);
      default:
        return GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandStatus.FAILURE)
            .gitCommandRequest(gitCommandRequest)
            .errorMessage(GIT_YAML_LOG_PREFIX + "Git Operation not supported")
            .build();
    }
  }

  private DelegateResponseData handleListRemote(
      GitCommandParams gitCommandParams, GitConfigDTO gitConfig, SshSessionConfig sshSessionConfig) {
    ListRemoteRequest listRemoteRequest = (ListRemoteRequest) gitCommandParams.getGitCommandRequest();
    log.info(GIT_YAML_LOG_PREFIX + "LIST_REMOTE: [{}]", listRemoteRequest);
    ListRemoteResult gitListRemoteResult =
        gitService.listRemote(gitConfig, listRemoteRequest, getAccountId(), sshSessionConfig, false);

    return GitCommandExecutionResponse.builder()
        .gitCommandRequest(listRemoteRequest)
        .gitCommandResult(gitListRemoteResult)
        .gitCommandStatus(GitCommandStatus.SUCCESS)
        .build();
  }

  private DelegateResponseData handleCommitAndPush(
      GitCommandParams gitCommandParams, GitConfigDTO gitConfig, SshSessionConfig sshSessionConfig) {
    CommitAndPushRequest gitCommitRequest = (CommitAndPushRequest) gitCommandParams.getGitCommandRequest();
    log.info(GIT_YAML_LOG_PREFIX + "COMMIT_AND_PUSH: [{}]", gitCommitRequest);
    CommitAndPushResult gitCommitAndPushResult =
        gitService.commitAndPush(gitConfig, gitCommitRequest, getAccountId(), sshSessionConfig, false);

    return GitCommandExecutionResponse.builder()
        .gitCommandRequest(gitCommitRequest)
        .gitCommandResult(gitCommitAndPushResult)
        .gitCommandStatus(GitCommandStatus.SUCCESS)
        .build();
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
