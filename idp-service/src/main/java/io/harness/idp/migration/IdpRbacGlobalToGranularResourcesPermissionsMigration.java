/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.migration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.idp.common.RbacConstants.IDP_ADVANCED_CONFIGURATION;
import static io.harness.idp.common.RbacConstants.IDP_ADVANCED_CONFIGURATION_DELETE;
import static io.harness.idp.common.RbacConstants.IDP_ADVANCED_CONFIGURATION_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_ADVANCED_CONFIGURATION_VIEW;
import static io.harness.idp.common.RbacConstants.IDP_CATALOG_ACCESS_POLICY;
import static io.harness.idp.common.RbacConstants.IDP_CATALOG_ACCESS_POLICY_CREATE;
import static io.harness.idp.common.RbacConstants.IDP_CATALOG_ACCESS_POLICY_DELETE;
import static io.harness.idp.common.RbacConstants.IDP_CATALOG_ACCESS_POLICY_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_CATALOG_ACCESS_POLICY_VIEW;
import static io.harness.idp.common.RbacConstants.IDP_INTEGRATION;
import static io.harness.idp.common.RbacConstants.IDP_INTEGRATION_CREATE;
import static io.harness.idp.common.RbacConstants.IDP_INTEGRATION_DELETE;
import static io.harness.idp.common.RbacConstants.IDP_INTEGRATION_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_INTEGRATION_VIEW;
import static io.harness.idp.common.RbacConstants.IDP_LAYOUT;
import static io.harness.idp.common.RbacConstants.IDP_LAYOUT_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_LAYOUT_VIEW;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN_DELETE;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN_TOGGLE;
import static io.harness.idp.common.RbacConstants.IDP_PLUGIN_VIEW;
import static io.harness.idp.common.RbacConstants.IDP_SCORECARD;
import static io.harness.idp.common.RbacConstants.IDP_SCORECARD_DELETE;
import static io.harness.idp.common.RbacConstants.IDP_SCORECARD_EDIT;
import static io.harness.idp.common.RbacConstants.IDP_SCORECARD_VIEW;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static lombok.AccessLevel.PRIVATE;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.roles.api.RoleDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.account.utils.AccountUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.migration.NGMigration;
import io.harness.ng.beans.PageResponse;
import io.harness.resourcegroup.v1.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.v2.model.ResourceFilter;
import io.harness.resourcegroup.v2.model.ResourceSelector;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class IdpRbacGlobalToGranularResourcesPermissionsMigration implements NGMigration {
  private static final String MIGRATION_PURPOSE =
      "idp rbac global resource permission to granular resources and permissions";
  private static final String IDP_SETTINGS_RESOURCE_TYPE = "IDP_SETTINGS";
  private static final String IDP_IDPSETTINGS_MANAGE_PERMISSION = "idp_idpsettings_manage";

  @Inject private AccountUtils accountUtils;
  @Inject @Named("PRIVILEGED") private AccessControlAdminClient accessControlAdminClient;
  @Inject @Named("PRIVILEGED") private ResourceGroupClient resourceGroupClient;

  @Override
  public void migrate() {
    log.info("Starting the migration for {}.", MIGRATION_PURPOSE);
    List<String> accountIdentifiers = accountUtils.getAllNGAccountIds();
    log.info("Fetched total of {} NG Accounts", accountIdentifiers.size());
    accountIdentifiers.forEach(accountIdentifier -> {
      log.info("Migrating {} for accountIdentifier = {}", MIGRATION_PURPOSE, accountIdentifier);
      rolesUpdate(accountIdentifier);
      resourceGroupsUpdate(accountIdentifier);
      log.info("Migrated {} for accountIdentifier = {}", MIGRATION_PURPOSE, accountIdentifier);
    });
    log.info("Completed the migration for {}.", MIGRATION_PURPOSE);
  }

  private void rolesUpdate(String accountIdentifier) {
    try {
      PageResponse<RoleResponseDTO> rolesResponse;
      int page = 0;
      do {
        try {
          rolesResponse =
              getResponse(accessControlAdminClient.getRoles(page, 100, accountIdentifier, null, null, null));
        } catch (Exception ex) {
          throw new UnexpectedException(
              "Error in fetching roles for accountIdentifier " + accountIdentifier + " | page " + page);
        }
        if (Objects.nonNull(rolesResponse) && isNotEmpty(rolesResponse.getContent())) {
          List<RoleResponseDTO> roleResponseDTOS = rolesResponse.getContent();
          roleResponseDTOS.forEach(roleResponseDTO -> {
            RoleDTO roleDTO = roleResponseDTO.getRole();
            boolean harnessManaged = roleResponseDTO.isHarnessManaged();
            String roleIdentifier = roleDTO.getIdentifier();
            Set<String> permissions = roleDTO.getPermissions();
            if (Boolean.FALSE.equals(harnessManaged) && permissions.contains(IDP_IDPSETTINGS_MANAGE_PERMISSION)) {
              log.info("Found global resource permission in role = {} account = {}", roleIdentifier, accountIdentifier);
              permissions.remove(IDP_IDPSETTINGS_MANAGE_PERMISSION);
              permissions.addAll(Arrays.asList(IDP_PLUGIN_VIEW, IDP_PLUGIN_EDIT, IDP_PLUGIN_TOGGLE, IDP_PLUGIN_DELETE,
                  IDP_SCORECARD_VIEW, IDP_SCORECARD_EDIT, IDP_SCORECARD_DELETE, IDP_LAYOUT_VIEW, IDP_LAYOUT_EDIT,
                  IDP_CATALOG_ACCESS_POLICY_VIEW, IDP_CATALOG_ACCESS_POLICY_CREATE, IDP_CATALOG_ACCESS_POLICY_EDIT,
                  IDP_CATALOG_ACCESS_POLICY_DELETE, IDP_INTEGRATION_VIEW, IDP_INTEGRATION_CREATE, IDP_INTEGRATION_EDIT,
                  IDP_INTEGRATION_DELETE, IDP_ADVANCED_CONFIGURATION_VIEW, IDP_ADVANCED_CONFIGURATION_EDIT,
                  IDP_ADVANCED_CONFIGURATION_DELETE));
              try {
                getResponse(
                    accessControlAdminClient.updateRole(roleIdentifier, accountIdentifier, null, null, roleDTO));
                log.info(
                    "Updated role = {} in account = {} with idp granular resources and permissions and removed global resource permission",
                    roleIdentifier, accountIdentifier);
              } catch (Exception ex) {
                log.error(
                    "Error updating role = {} in account = {} with idp granular resources and permissions and removing global resource permission",
                    roleIdentifier, accountIdentifier);
              }
            }
          });
        }
        page++;
      } while (rolesResponse != null && isNotEmpty(rolesResponse.getContent()));
    } catch (Exception ex) {
      log.error("Error in roles update for accountIdentifier = {}", accountIdentifier, ex);
    }
  }

  private void resourceGroupsUpdate(String accountIdentifier) {
    try {
      PageResponse<ResourceGroupResponse> resourceGroupsResponse;
      int page = 0;
      do {
        try {
          resourceGroupsResponse = getResponse(resourceGroupClient.getFilteredResourceGroups(
              ResourceGroupFilterDTO.builder().accountIdentifier(accountIdentifier).build(), accountIdentifier, page,
              100));
        } catch (Exception ex) {
          throw new UnexpectedException(
              "Error in fetching resource groups for accountIdentifier " + accountIdentifier + " | page " + page);
        }
        if (Objects.nonNull(resourceGroupsResponse) && isNotEmpty(resourceGroupsResponse.getContent())) {
          List<ResourceGroupResponse> resourceGroupResponses = resourceGroupsResponse.getContent();
          resourceGroupResponses.forEach(resourceGroupResponse -> {
            ResourceGroupDTO resourceGroupDTO = resourceGroupResponse.getResourceGroup();
            boolean harnessManaged = resourceGroupResponse.isHarnessManaged();
            ResourceFilter resourceFilter = resourceGroupDTO.getResourceFilter();
            String resourceGroupIdentifier = resourceGroupDTO.getIdentifier();
            List<ResourceSelector> resourceSelectors = resourceFilter.getResources();
            if (isNotEmpty(resourceSelectors)) {
              for (ResourceSelector resourceSelector : resourceSelectors) {
                if (Boolean.FALSE.equals(harnessManaged)
                    && resourceSelector.getResourceType().equals(IDP_SETTINGS_RESOURCE_TYPE)) {
                  log.info("Found global resource in resource group = {} account = {}", resourceGroupIdentifier,
                      accountIdentifier);
                  List<ResourceSelector> resourceSelectorsUpdated = new ArrayList<>(resourceSelectors);
                  resourceSelectorsUpdated.remove(resourceSelector);
                  resourceSelectorsUpdated.add(ResourceSelector.builder().resourceType(IDP_PLUGIN).build());
                  resourceSelectorsUpdated.add(ResourceSelector.builder().resourceType(IDP_SCORECARD).build());
                  resourceSelectorsUpdated.add(ResourceSelector.builder().resourceType(IDP_LAYOUT).build());
                  resourceSelectorsUpdated.add(
                      ResourceSelector.builder().resourceType(IDP_CATALOG_ACCESS_POLICY).build());
                  resourceSelectorsUpdated.add(ResourceSelector.builder().resourceType(IDP_INTEGRATION).build());
                  resourceSelectorsUpdated.add(
                      ResourceSelector.builder().resourceType(IDP_ADVANCED_CONFIGURATION).build());
                  resourceFilter.setResources(resourceSelectorsUpdated);
                  resourceGroupDTO.setResourceFilter(resourceFilter);
                  try {
                    getResponse(
                        resourceGroupClient.updateResourceGroup(resourceGroupDTO.getIdentifier(), accountIdentifier,
                            null, null, ResourceGroupRequest.builder().resourceGroup(resourceGroupDTO).build()));
                    log.info(
                        "Updated resource group = {} in account = {} with idp granular resources and removed global resource",
                        resourceGroupIdentifier, accountIdentifier);
                  } catch (Exception ex) {
                    log.error(
                        "Error updated resource group = {} in account = {} with idp granular resources and removing global resource",
                        resourceGroupIdentifier, accountIdentifier);
                  }
                }
              }
            }
          });
        }
        page++;
      } while (resourceGroupsResponse != null && isNotEmpty(resourceGroupsResponse.getContent()));
    } catch (Exception ex) {
      log.error("Error in resource groups update for accountIdentifier = {}", accountIdentifier, ex);
    }
  }
}
