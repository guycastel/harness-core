/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ASHISHSANODIA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.services.ScopeInfoService;
import io.harness.rule.Owner;

import io.dropwizard.util.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.cache.Cache;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class ScopeInfoServiceImplTest {
  private final OrganizationService organizationService = mock(OrganizationService.class);
  private final ProjectService projectService = mock(ProjectService.class);
  private final ScopeInfoHelper scopeInfoHelper = new ScopeInfoHelper();
  private final Cache<String, ScopeInfo> scopeInfoCache = mock(Cache.class);
  private final ScopeInfoService scopeResolverService =
      new ScopeInfoServiceImpl(organizationService, projectService, scopeInfoHelper, scopeInfoCache);
  String accountIdentifier = randomAlphabetic(10);
  String orgUniqueId = randomAlphabetic(10);
  String orgIdentifier = randomAlphabetic(10);
  String projectUniqueId = randomAlphabetic(10);
  String projectIdentifier = randomAlphabetic(10);

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void getAccountScopeInfoWithUniqueIdFromCache() {
    when(scopeInfoCache.containsKey(scopeInfoHelper.getScopeInfoUniqueIdCacheKey(accountIdentifier))).thenReturn(true);
    when(scopeInfoCache.get(scopeInfoHelper.getScopeInfoUniqueIdCacheKey(accountIdentifier)))
        .thenReturn(ScopeInfo.builder()
                        .accountIdentifier(accountIdentifier)
                        .uniqueId(accountIdentifier)
                        .scopeType(ScopeLevel.ACCOUNT)
                        .build());

    Map<String, Optional<ScopeInfo>> scopeInfoMap =
        scopeResolverService.getScopeInfo(accountIdentifier, Sets.of(accountIdentifier));

    assertThat(scopeInfoMap.containsKey(accountIdentifier)).isTrue();

    Optional<ScopeInfo> scopeInfo = scopeInfoMap.get(accountIdentifier);
    verify(scopeInfoCache, times(1)).get(scopeInfoHelper.getScopeInfoUniqueIdCacheKey(accountIdentifier));
    assertThat(scopeInfo.isPresent()).isTrue();
    assertThat(scopeInfo.get().getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(scopeInfo.get().getUniqueId()).isEqualTo(accountIdentifier);
    assertThat(scopeInfo.get().getScopeType()).isEqualTo(ScopeLevel.ACCOUNT);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void getAccountScopeInfoWithUniqueId() {
    Map<String, Optional<ScopeInfo>> scopeInfoMap =
        scopeResolverService.getScopeInfo(accountIdentifier, Sets.of(accountIdentifier));

    assertThat(scopeInfoMap.containsKey(accountIdentifier)).isTrue();

    Optional<ScopeInfo> scopeInfo = scopeInfoMap.get(accountIdentifier);
    assertThat(scopeInfo.isPresent()).isTrue();
    assertThat(scopeInfo.get().getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(scopeInfo.get().getUniqueId()).isEqualTo(accountIdentifier);
    assertThat(scopeInfo.get().getScopeType()).isEqualTo(ScopeLevel.ACCOUNT);

    verify(scopeInfoCache, times(1)).containsKey(scopeInfoHelper.getScopeInfoUniqueIdCacheKey(accountIdentifier));
    verify(scopeInfoCache, times(0)).get(anyString());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void getProjectScopeInfoWithUniqueId() {
    when(projectService.get(projectUniqueId))
        .thenReturn(Optional.of(Project.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(projectIdentifier)
                                    .uniqueId(projectUniqueId)
                                    .parentUniqueId(orgUniqueId)
                                    .build()));
    when(organizationService.get(orgUniqueId))
        .thenReturn(Optional.of(Organization.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(orgIdentifier)
                                    .uniqueId(orgUniqueId)
                                    .parentUniqueId(accountIdentifier)
                                    .build()));

    Map<String, Optional<ScopeInfo>> scopeInfoMap =
        scopeResolverService.getScopeInfo(accountIdentifier, Sets.of(projectUniqueId));

    assertThat(scopeInfoMap.containsKey(projectUniqueId)).isTrue();

    Optional<ScopeInfo> scopeInfo = scopeInfoMap.get(projectUniqueId);
    assertThat(scopeInfo.isPresent()).isTrue();
    assertThat(scopeInfo.get().getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(scopeInfo.get().getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(scopeInfo.get().getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(scopeInfo.get().getUniqueId()).isEqualTo(projectUniqueId);
    assertThat(scopeInfo.get().getScopeType()).isEqualTo(ScopeLevel.PROJECT);

    verify(scopeInfoCache, times(1)).containsKey(scopeInfoHelper.getScopeInfoUniqueIdCacheKey(projectUniqueId));
    verify(scopeInfoCache, times(0)).get(anyString());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void getOrgScopeInfoWithUniqueId() {
    when(projectService.get(orgUniqueId)).thenReturn(Optional.empty());
    when(organizationService.get(orgUniqueId))
        .thenReturn(Optional.of(Organization.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(orgIdentifier)
                                    .uniqueId(orgUniqueId)
                                    .parentUniqueId(accountIdentifier)
                                    .build()));

    Map<String, Optional<ScopeInfo>> scopeInfoMap =
        scopeResolverService.getScopeInfo(accountIdentifier, Sets.of(orgUniqueId));

    assertThat(scopeInfoMap.containsKey(orgUniqueId)).isTrue();

    Optional<ScopeInfo> scopeInfo = scopeInfoMap.get(orgUniqueId);
    assertThat(scopeInfo.isPresent()).isTrue();
    assertThat(scopeInfo.get().getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(scopeInfo.get().getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(scopeInfo.get().getProjectIdentifier()).isNull();
    assertThat(scopeInfo.get().getUniqueId()).isEqualTo(orgUniqueId);
    assertThat(scopeInfo.get().getScopeType()).isEqualTo(ScopeLevel.ORGANIZATION);

    verify(scopeInfoCache, times(1)).containsKey(scopeInfoHelper.getScopeInfoUniqueIdCacheKey(orgUniqueId));
    verify(scopeInfoCache, times(0)).get(anyString());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void getScopeInfoMap() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String orgUniqueId1 = "orgUniqueId-1";
    String orgUniqueId2 = "orgUniqueId-2";
    String orgUniqueId3 = "orgUniqueId-3";
    when(projectService.get(orgUniqueId1)).thenReturn(Optional.empty());
    when(organizationService.get(orgUniqueId1))
        .thenReturn(Optional.of(Organization.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(orgIdentifier)
                                    .uniqueId(orgUniqueId1)
                                    .parentUniqueId(accountIdentifier)
                                    .build()));
    when(projectService.get(orgUniqueId2)).thenReturn(Optional.empty());
    when(organizationService.get(orgUniqueId2))
        .thenReturn(Optional.of(Organization.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(orgIdentifier)
                                    .uniqueId(orgUniqueId2)
                                    .parentUniqueId(accountIdentifier)
                                    .build()));
    when(projectService.get(orgUniqueId3)).thenReturn(Optional.empty());
    when(organizationService.get(orgUniqueId3))
        .thenReturn(Optional.of(Organization.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(orgIdentifier)
                                    .uniqueId(orgUniqueId3)
                                    .parentUniqueId(accountIdentifier)
                                    .build()));

    Set<String> uniqueIds = Set.of(orgUniqueId1, orgUniqueId2, orgUniqueId3);

    Map<String, Optional<ScopeInfo>> scopeInfoMap = scopeResolverService.getScopeInfo(accountIdentifier, uniqueIds);

    assertThat(scopeInfoMap.containsKey(orgUniqueId1)).isTrue();
    assertThat(scopeInfoMap.containsKey(orgUniqueId2)).isTrue();
    assertThat(scopeInfoMap.containsKey(orgUniqueId3)).isTrue();

    assertThat(scopeInfoMap.get(orgUniqueId1).orElseThrow().getUniqueId()).isEqualTo(orgUniqueId1);
    assertThat(scopeInfoMap.get(orgUniqueId2).orElseThrow().getUniqueId()).isEqualTo(orgUniqueId2);
    assertThat(scopeInfoMap.get(orgUniqueId3).orElseThrow().getUniqueId()).isEqualTo(orgUniqueId3);
  }
}
