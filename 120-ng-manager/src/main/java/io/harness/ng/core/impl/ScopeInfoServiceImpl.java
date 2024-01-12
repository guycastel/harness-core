/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.services.ScopeInfoService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ScopeInfoServiceImpl implements ScopeInfoService {
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final ScopeInfoHelper scopeInfoHelper;
  private final Cache<String, ScopeInfo> scopeInfoCache;

  @Inject
  public ScopeInfoServiceImpl(OrganizationService organizationService, ProjectService projectService,
      ScopeInfoHelper scopeInfoHelper, @Named(SCOPE_INFO_UNIQUE_ID_CACHE_KEY) Cache<String, ScopeInfo> scopeInfoCache) {
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.scopeInfoHelper = scopeInfoHelper;
    this.scopeInfoCache = scopeInfoCache;
  }

  @Override
  public Optional<ScopeInfo> getScopeInfo(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (isEmpty(orgIdentifier) && isEmpty(projectIdentifier)) {
      return Optional.of(
          scopeInfoHelper.populateScopeInfo(ScopeLevel.ACCOUNT, accountIdentifier, accountIdentifier, null, null));
    }

    if (isEmpty(projectIdentifier)) {
      return organizationService.getScopeInfo(accountIdentifier, orgIdentifier);
    }

    return projectService.getScopeInfo(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public Map<String, Optional<ScopeInfo>> getScopeInfo(String accountIdentifier, Set<String> uniqueIds) {
    return uniqueIds.stream().collect(
        toMap(Function.identity(), uniqueId -> getScopeInfo(accountIdentifier, uniqueId)));
  }

  private Optional<ScopeInfo> getScopeInfo(String accountIdentifier, String uniqueId) {
    String scopeInfoUniqueIdCacheKey = scopeInfoHelper.getScopeInfoUniqueIdCacheKey(uniqueId);

    // If cache contains fetch from cache and return
    if (scopeInfoCache.containsKey(scopeInfoUniqueIdCacheKey)) {
      return Optional.of(scopeInfoCache.get(scopeInfoUniqueIdCacheKey));
    }

    ScopeInfo scopeInfo;
    if (Objects.equals(accountIdentifier, uniqueId)) {
      scopeInfo = ScopeInfo.builder()
                      .accountIdentifier(accountIdentifier)
                      .uniqueId(accountIdentifier)
                      .scopeType(ScopeLevel.ACCOUNT)
                      .build();

      return Optional.of(scopeInfo);
    }

    // fetch project with unique id
    Optional<Project> project = projectService.get(uniqueId);
    if (project.isPresent()) {
      Project existingProject = project.get();
      // fetch project's parent org id
      Optional<Organization> organization = organizationService.get(existingProject.getParentUniqueId());
      Organization existingOrganization = organization.orElseThrow();

      scopeInfo = ScopeInfo.builder()
                      .accountIdentifier(existingOrganization.getAccountIdentifier())
                      .orgIdentifier(existingOrganization.getIdentifier())
                      .projectIdentifier(existingProject.getIdentifier())
                      .uniqueId(uniqueId)
                      .scopeType(ScopeLevel.PROJECT)
                      .build();
      scopeInfoCache.put(scopeInfoUniqueIdCacheKey, scopeInfo);

      return Optional.of(scopeInfo);
    }

    // fetch org with unique id
    Optional<Organization> organization = organizationService.get(uniqueId);
    if (organization.isPresent()) {
      Organization existingOrganization = organization.get();

      scopeInfo = ScopeInfo.builder()
                      .accountIdentifier(existingOrganization.getAccountIdentifier())
                      .orgIdentifier(existingOrganization.getIdentifier())
                      .uniqueId(uniqueId)
                      .scopeType(ScopeLevel.ORGANIZATION)
                      .build();
      scopeInfoCache.put(scopeInfoUniqueIdCacheKey, scopeInfo);

      return Optional.of(scopeInfo);
    }

    return Optional.empty();
  }
}
