/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ASHISHSANODIA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.impl.ScopeInfoHelper;
import io.harness.ng.core.impl.ScopeInfoServiceImpl;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.services.ScopeInfoService;
import io.harness.rule.Owner;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.cache.Cache;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class ScopeInfoResourceTest {
  private final OrganizationService organizationService = mock(OrganizationService.class);
  private final ProjectService projectService = mock(ProjectService.class);
  private final ScopeInfoHelper scopeInfoHelper = mock(ScopeInfoHelper.class);
  private final Cache<String, ScopeInfo> scopeInfoCache = mock(Cache.class);
  private final ScopeInfoService scopeResolverService =
      new ScopeInfoServiceImpl(organizationService, projectService, scopeInfoHelper, scopeInfoCache);
  private final ScopeInfoResource scopeInfoResource = new ScopeInfoResource(scopeResolverService);

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

    ResponseDTO<Map<String, Optional<ScopeInfo>>> scopeInfo =
        scopeInfoResource.getScopeInfo(accountIdentifier, uniqueIds);

    Map<String, Optional<ScopeInfo>> scopeInfoMap = scopeInfo.getData();

    assertThat(scopeInfoMap.containsKey(orgUniqueId1)).isTrue();
    assertThat(scopeInfoMap.containsKey(orgUniqueId2)).isTrue();
    assertThat(scopeInfoMap.containsKey(orgUniqueId3)).isTrue();

    assertThat(scopeInfoMap.get(orgUniqueId1).orElseThrow().getUniqueId()).isEqualTo(orgUniqueId1);
    assertThat(scopeInfoMap.get(orgUniqueId2).orElseThrow().getUniqueId()).isEqualTo(orgUniqueId2);
    assertThat(scopeInfoMap.get(orgUniqueId3).orElseThrow().getUniqueId()).isEqualTo(orgUniqueId3);
  }
}
