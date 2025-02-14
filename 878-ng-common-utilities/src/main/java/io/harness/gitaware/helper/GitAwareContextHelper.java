/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitaware.helper;
import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import static java.lang.Boolean.TRUE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.context.GlobalContext;
import io.harness.data.structure.EmptyPredicate;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.scm.beans.ScmGitMetaDataContext;
import io.harness.gitsync.sdk.CacheResponse;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitx.EntityGitInfo;
import io.harness.logging.AutoLogContext;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.gitaware.GitAware;

import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@UtilityClass
@OwnedBy(DX)
public class GitAwareContextHelper {
  public static final String DEFAULT = "__default__";

  public GitEntityInfo getGitRequestParamsInfo() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      return GitEntityInfo.builder().build();
    }
    return gitSyncBranchContext.getGitBranchInfo();
  }

  public void initDefaultScmGitMetaDataAndRequestParams() {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(
        GitSyncBranchContext.builder().gitBranchInfo(GitEntityInfo.builder().build()).build());
    GlobalContextManager.upsertGlobalContextRecord(
        ScmGitMetaDataContext.builder().scmGitMetaData(ScmGitMetaData.builder().build()).build());
  }

  public void initDefaultScmGitMetaData() {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(
        ScmGitMetaDataContext.builder().scmGitMetaData(ScmGitMetaData.builder().build()).build());
  }

  public ScmGitMetaData getScmGitMetaData() {
    ScmGitMetaDataContext gitMetaDataContext = GlobalContextManager.get(ScmGitMetaDataContext.NG_GIT_SYNC_CONTEXT);
    if (gitMetaDataContext == null) {
      return ScmGitMetaData.builder().build();
    }
    return gitMetaDataContext.getScmGitMetaData();
  }

  public void updateScmGitMetaData(ScmGitMetaData scmGitMetaData) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(
        ScmGitMetaDataContext.builder().scmGitMetaData(scmGitMetaData).build());
  }

  public boolean isOldFlow() {
    GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
    return gitEntityInfo == null || gitEntityInfo.getStoreType() == null;
  }

  public EntityGitDetails getEntityGitDetailsFromScmGitMetadata() {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();
    if (scmGitMetaData == null) {
      return EntityGitDetails.builder().build();
    }
    return EntityGitDetails.builder()
        .objectId(scmGitMetaData.getBlobId())
        .branch(scmGitMetaData.getBranchName())
        .repoName(scmGitMetaData.getRepoName())
        .filePath(scmGitMetaData.getFilePath())
        .commitId(scmGitMetaData.getCommitId())
        .fileUrl(scmGitMetaData.getFileUrl())
        .build();
  }

  public EntityGitDetails updateEntityGitDetailsFromScmGitMetadata(@NonNull EntityGitDetails existingDetails) {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();

    if (scmGitMetaData == null) {
      return existingDetails;
    }

    if (isEmpty(existingDetails.getObjectId())) {
      existingDetails.setObjectId(scmGitMetaData.getBlobId());
    }
    if (isEmpty(existingDetails.getBranch())) {
      existingDetails.setBranch(scmGitMetaData.getBranchName());
    }
    if (isEmpty(existingDetails.getRepoName())) {
      existingDetails.setRepoName(scmGitMetaData.getRepoName());
    }
    if (isEmpty(existingDetails.getFilePath())) {
      existingDetails.setFilePath(scmGitMetaData.getFilePath());
    }
    if (isEmpty(existingDetails.getCommitId())) {
      existingDetails.setCommitId(scmGitMetaData.getCommitId());
    }
    if (isEmpty(existingDetails.getFileUrl())) {
      existingDetails.setFileUrl(scmGitMetaData.getFileUrl());
    }

    return existingDetails;
  }

  public EntityGitInfo updateEntityGitInfoFromScmGitMetadata(@NonNull EntityGitInfo entityGitInfo) {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();

    if (scmGitMetaData == null) {
      return entityGitInfo;
    }

    if (isEmpty(entityGitInfo.getObjectId())) {
      entityGitInfo.setObjectId(scmGitMetaData.getBlobId());
    }
    if (isEmpty(entityGitInfo.getBranch())) {
      entityGitInfo.setBranch(scmGitMetaData.getBranchName());
    }
    if (isEmpty(entityGitInfo.getRepoName())) {
      entityGitInfo.setRepoName(scmGitMetaData.getRepoName());
    }
    if (isEmpty(entityGitInfo.getFilePath())) {
      entityGitInfo.setFilePath(scmGitMetaData.getFilePath());
    }
    if (isEmpty(entityGitInfo.getCommitId())) {
      entityGitInfo.setCommitId(scmGitMetaData.getCommitId());
    }
    if (isEmpty(entityGitInfo.getFileUrl())) {
      entityGitInfo.setFileUrl(scmGitMetaData.getFileUrl());
    }

    return entityGitInfo;
  }

  public CacheResponse getCacheResponseFromScmGitMetadata() {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();
    if (scmGitMetaData == null || scmGitMetaData.getCacheResponse() == null) {
      return null;
    }
    return scmGitMetaData.getCacheResponse();
  }

  public EntityGitDetails getEntityGitDetails(GitAware gitAware) {
    return EntityGitDetails.builder().repoName(gitAware.getRepo()).filePath(gitAware.getFilePath()).build();
  }

  public EntityGitInfo getEntityInfo(GitAware gitAware) {
    return EntityGitInfo.builder().repoName(gitAware.getRepo()).filePath(gitAware.getFilePath()).build();
  }

  public String getBranchInRequest() {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    return gitEntityInfo == null ? "" : gitEntityInfo.getBranch();
  }

  public String getFilepathInRequest() {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    return gitEntityInfo == null ? "" : gitEntityInfo.getFilePath();
  }

  public boolean isNullOrDefault(String val) {
    return isEmpty(val) || val.equals(DEFAULT);
  }

  public void updateGitEntityContext(GitEntityInfo branchInfo) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(branchInfo).build());
  }

  public static void populateGitDetails(GitEntityInfo gitEntityInfo) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(gitEntityInfo).build());
  }

  public void updateGitEntityContextWithBranch(String branch) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    gitEntityInfo.setBranch(branch);
    updateGitEntityContext(gitEntityInfo);
  }

  public String getBranchInSCMGitMetadata() {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();
    if (scmGitMetaData == null) {
      return null;
    }
    return scmGitMetaData.getBranchName();
  }

  public void setIsDefaultBranchInGitEntityInfo() {
    GitEntityInfo gitEntityInfo = getGitRequestParamsInfo();

    if (gitEntityInfo != null) {
      gitEntityInfo.setIsDefaultBranch(isEmpty(gitEntityInfo.getBranch()));
    }
  }

  public boolean getIsDefaultBranchFromGitEntityInfo() {
    GitEntityInfo gitEntityInfo = getGitRequestParamsInfo();

    if (gitEntityInfo != null) {
      return gitEntityInfo.getIsDefaultBranch();
    }

    return false;
  }

  public static void setIsDefaultBranchInGitEntityInfoWithParameter(String branch) {
    GitEntityInfo gitEntityInfo = getGitRequestParamsInfo();

    if (gitEntityInfo != null) {
      gitEntityInfo.setIsDefaultBranch(isEmpty(branch));
    }
  }

  public AutoLogContext autoLogContext() {
    Map<String, String> contextMap = new HashMap<>();
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext != null && gitSyncBranchContext.getGitBranchInfo() != null) {
      GitEntityInfo gitBranchInfo = gitSyncBranchContext.getGitBranchInfo();
      contextMap.put("GitBranchName", gitBranchInfo.getBranch());
      contextMap.put("GitConnectorRef", gitBranchInfo.getConnectorRef());
      contextMap.put("GitRepoName", gitBranchInfo.getRepoName());
      contextMap.put("GitParentConnectorRef", gitBranchInfo.getParentEntityConnectorRef());
      contextMap.put("GitParentRepoName", gitBranchInfo.getParentEntityRepoName());
    }
    return new AutoLogContext(contextMap, OVERRIDE_NESTS);
  }

  public boolean isRemoteEntity(GitEntityInfo gitEntityInfo) {
    return gitEntityInfo != null && StoreType.REMOTE.equals(gitEntityInfo.getStoreType());
  }

  public boolean isRemoteEntity() {
    GitEntityInfo gitEntityInfo = getGitRequestParamsInfo();
    return isRemoteEntity(gitEntityInfo);
  }

  public boolean isDefaultBranch() {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    if (gitEntityInfo != null && gitEntityInfo.getIsDefaultBranch() != null) {
      return gitEntityInfo.getIsDefaultBranch();
    }
    return false;
  }

  public String getBranchFromGitContext() {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    if (gitEntityInfo != null) {
      return gitEntityInfo.getBranch();
    }
    return "";
  }

  public StoreType getStoreTypeFromGitContext() {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    if (gitEntityInfo != null) {
      return gitEntityInfo.getStoreType();
    }
    return null;
  }

  public boolean isTransientBranchSet() {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    if (gitEntityInfo != null) {
      return isPresent(gitEntityInfo.getTransientBranch());
    }
    return false;
  }

  public void setTransientBranch(String transientBranch) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    if (gitEntityInfo != null) {
      gitEntityInfo.setTransientBranch(transientBranch);
      updateGitEntityContext(gitEntityInfo);
    }
  }

  public String getBranchInRequestOrFromSCMGitMetadata() {
    String branch = getBranchInRequest();
    return !EmptyPredicate.isEmpty(branch) ? branch : getBranchInSCMGitMetadata();
  }

  public void resetTransientBranch() {
    setTransientBranch(null);
  }

  private boolean isPresent(String val) {
    return !isEmpty(val) && !DEFAULT.equals(val);
  }

  public boolean isGitDefaultBranch() {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();
    if (scmGitMetaData == null) {
      return false;
    }
    return Boolean.TRUE.equals(scmGitMetaData.getIsGitDefaultBranch());
  }

  public void setHarnessCodeConnectorRef() {
    GitEntityInfo gitEntityInfo = getGitRequestParamsInfo();
    if (TRUE.equals(gitEntityInfo.getIsHarnessCodeRepo())) {
      gitEntityInfo.setConnectorRef(GitSyncConstants.EMPTY);
      GitAwareContextHelper.updateGitEntityContext(gitEntityInfo);
    }
  }
}
