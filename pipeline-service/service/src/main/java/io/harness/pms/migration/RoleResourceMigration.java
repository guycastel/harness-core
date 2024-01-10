/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.migration;

import static io.harness.authorization.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.beans.FeatureName.CDS_INPUT_SET_MIGRATION;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.account.utils.AccountUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ff.FeatureFlagService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.project.remote.ProjectClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.v1.remote.dto.ManagedFilter;
import io.harness.resourcegroup.v1.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.v2.model.ResourceFilter;
import io.harness.resourcegroup.v2.model.ResourceSelector;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDC)
public class RoleResourceMigration implements Runnable {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AccountUtils accountUtils;
  @Inject private PersistentLocker persistentLocker;
  private final String DEBUG_MESSAGE = "ProjectOrgBasicRoleCreationJob: ";
  private static final String LOCK_NAME = "ProjectOrgBasicRoleCreationJobLock";
  @Inject private PMSInputSetService pmsInputSetService;
  @Inject private ResourceGroupClient resourceGroupClient;
  @Inject private ProjectClient projectClient;
  private final String PIPELINE_RESOURCE_TYPE = "PIPELINE";
  private final String INPUT_RESOURCE_TYPE = "INPUT_SET";
  @Inject @Named("roleMigrationCache") private Cache<String, Boolean> eventsCache;

  @Override
  public void run() {
    log.info(DEBUG_MESSAGE + "started...");
    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(LOCK_NAME, Duration.ofSeconds(5))) {
      if (lock == null) {
        log.info(DEBUG_MESSAGE + "failed to acquire lock");
        return;
      }
      try {
        SecurityContextBuilder.setContext(new ServicePrincipal(PIPELINE_SERVICE.getServiceId()));
        execute();
      } catch (Exception ex) {
        log.error(DEBUG_MESSAGE + " unexpected error occurred while Setting SecurityContext", ex);
      } finally {
        SecurityContextBuilder.unsetCompleteContext();
      }
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + " failed to acquire lock", ex);
    }
  }

  @VisibleForTesting
  void execute() {
    Set<String> targetAccounts = getAccountsForFFEnabled();
    for (String accountId : targetAccounts) {
      if (!eventsCache.containsKey(accountId)) {
        List<ProjectResponse> projects;
        int page = 0;
        while (true) {
          projects =
              getResponse(projectClient.listProject(accountId, null, false, null, null, page, 500, null)).getContent();
          if (EmptyPredicate.isEmpty(projects)) {
            break;
          }
          page++;
          List<ResourceGroupResponse> resourceGroupResponses = getResourceGroupResource(accountId, projects);
          if (EmptyPredicate.isNotEmpty(resourceGroupResponses)) {
            for (int i = 0; i < resourceGroupResponses.size(); i++) {
              ResourceGroupResponse response = resourceGroupResponses.get(i);
              if (null == response.getResourceGroup().getResourceFilter().getResources()) {
                break;
              }
              List<ResourceSelector> resourceSelectorList =
                  response.getResourceGroup().getResourceFilter().getResources();
              for (int j = 0; j < resourceSelectorList.size(); j++) {
                validateAndUpdateInputSetPermission(response, resourceSelectorList.get(j));
              }
            }
          }
        }
        eventsCache.put(accountId, true);
      }
    }
  }

  private void validateAndUpdateInputSetPermission(ResourceGroupResponse response, ResourceSelector resourceSelector) {
    if (resourceSelector.getResourceType().equals(PIPELINE_RESOURCE_TYPE)) {
      ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder = createResourceGroupDTOBuilder(response);
      List<ResourceSelector> resourceSelectorList = response.getResourceGroup().getResourceFilter().getResources();
      if (resourceSelector.getIdentifiers() != null) {
        createSpecificInputSetPermission(response, resourceSelector, resourceSelectorList, resourceGroupDTOBuilder);
      } else {
        createAllInputSetPermission(response, resourceSelectorList, resourceGroupDTOBuilder);
      }
    }
  }

  private void createSpecificInputSetPermission(ResourceGroupResponse response, ResourceSelector resourceSelector,
      List<ResourceSelector> resourceSelectorList, ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder) {
    for (String identifier : resourceSelector.getIdentifiers()) {
      Criteria criteria = createCriteria(response);
      criteria.and(InputSetEntityKeys.pipelineIdentifier).is(identifier);
      List<InputSetEntity> inputSetEntities = pmsInputSetService.list(criteria);
      List<String> inputSetIdentifiers = new ArrayList<>();
      if (!inputSetEntities.isEmpty()) {
        inputSetEntities.forEach(
            inputSetEntity -> inputSetIdentifiers.add(identifier + "-" + inputSetEntity.getIdentifier()));
        ResourceSelector resourceSelectorNew =
            ResourceSelector.builder().resourceType(INPUT_RESOURCE_TYPE).identifiers(inputSetIdentifiers).build();
        if (!resourceSelectorList.contains(resourceSelectorNew)) {
          resourceSelectorList.add(
              ResourceSelector.builder().resourceType(INPUT_RESOURCE_TYPE).identifiers(inputSetIdentifiers).build());
          resourceGroupDTOBuilder.resourceFilter(ResourceFilter.builder().resources(resourceSelectorList).build());
          updateResourceGroup(response, resourceGroupDTOBuilder);
        }
      }
    }
  }

  private void createAllInputSetPermission(ResourceGroupResponse response, List<ResourceSelector> resourceSelectorList,
      ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder) {
    ResourceSelector resourceSelector = ResourceSelector.builder().resourceType(INPUT_RESOURCE_TYPE).build();
    if (!resourceSelectorList.contains(resourceSelector)) {
      resourceSelectorList.add(resourceSelector);
      resourceGroupDTOBuilder.resourceFilter(ResourceFilter.builder().resources(resourceSelectorList).build());
      updateResourceGroup(response, resourceGroupDTOBuilder);
    }
  }

  private void updateResourceGroup(
      ResourceGroupResponse response, ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder) {
    Optional<ResourceGroupResponse> resourceGroupResponse =
        Optional.ofNullable(NGRestUtils.getResponse(resourceGroupClient.updateResourceGroup(
            response.getResourceGroup().getIdentifier(), response.getResourceGroup().getAccountIdentifier(),
            response.getResourceGroup().getOrgIdentifier(), response.getResourceGroup().getProjectIdentifier(),
            ResourceGroupRequest.builder().resourceGroup(resourceGroupDTOBuilder.build()).build())));
    resourceGroupResponse.get();
  }
  private Criteria createCriteria(ResourceGroupResponse response) {
    Criteria criteria = new Criteria();
    criteria.and(InputSetEntityKeys.accountId).is(response.getResourceGroup().getAccountIdentifier());
    criteria.and(InputSetEntityKeys.orgIdentifier).is(response.getResourceGroup().getOrgIdentifier());
    criteria.and(InputSetEntityKeys.projectIdentifier).is(response.getResourceGroup().getProjectIdentifier());
    criteria.and(InputSetEntityKeys.deleted).is(false);
    return criteria;
  }

  private ResourceGroupDTO.ResourceGroupDTOBuilder createResourceGroupDTOBuilder(ResourceGroupResponse response) {
    return ResourceGroupDTO.builder()
        .identifier(response.getResourceGroup().getIdentifier())
        .name(response.getResourceGroup().getName())
        .projectIdentifier(response.getResourceGroup().getProjectIdentifier())
        .orgIdentifier(response.getResourceGroup().getOrgIdentifier())
        .accountIdentifier(response.getResourceGroup().getAccountIdentifier())
        .includedScopes(response.getResourceGroup().getIncludedScopes())
        .tags(response.getResourceGroup().getTags())
        .color(response.getResourceGroup().getColor())
        .description(response.getResourceGroup().getDescription())
        .allowedScopeLevels(Sets.newHashSet(
            ScopeLevel
                .of(response.getResourceGroup().getAccountIdentifier(), response.getResourceGroup().getOrgIdentifier(),
                    response.getResourceGroup().getProjectIdentifier())
                .toString()
                .toLowerCase()));
  }

  private Set<String> getAccountsForFFEnabled() {
    try {
      return featureFlagService.getAccountIds(CDS_INPUT_SET_MIGRATION);
    } catch (Exception ex) {
      log.error("Failed to filter accounts for FF PIE_INPUT_SET_MIGRATION");
    }
    return null;
  }

  private List<ResourceGroupResponse> getResourceGroupResource(String accountId, List<ProjectResponse> projects) {
    List<ResourceGroupResponse> resourceGroupResponses = new ArrayList<>();
    for (ProjectResponse projectResponse : projects) {
      ResourceGroupFilterDTO resourceGroupFilterDTO =
          ResourceGroupFilterDTO.builder()
              .accountIdentifier(accountId)
              .orgIdentifier(projectResponse.getProject().getOrgIdentifier())
              .projectIdentifier(projectResponse.getProject().getIdentifier())
              .managedFilter(ManagedFilter.ONLY_CUSTOM)
              .build();
      resourceGroupResponses =
          getResponse(resourceGroupClient.getFilteredResourceGroups(resourceGroupFilterDTO, accountId, 0, 5000))
              .getContent();
    }
    return resourceGroupResponses;
  }
}
