/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.resource;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.MANDATE_CUSTOM_WEBHOOK_AUTHORIZATION;
import static io.harness.ngtriggers.Constants.MANDATE_PIPELINE_CREATE_EDIT_PERMISSION_TO_CREATE_EDIT_TRIGGERS;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.EntityNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.PollingTriggerStatusUpdateDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngsettings.client.remote.NGSettingsClient;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.BulkTriggersRequestDTO;
import io.harness.ngtriggers.beans.dto.BulkTriggersResponseDTO;
import io.harness.ngtriggers.beans.dto.NGTriggerCatalogDTO;
import io.harness.ngtriggers.beans.dto.NGTriggerDetailsResponseDTO;
import io.harness.ngtriggers.beans.dto.NGTriggerEventHistoryDTO;
import io.harness.ngtriggers.beans.dto.NGTriggerResponseDTO;
import io.harness.ngtriggers.beans.dto.NGTriggersFilterPropertiesDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.TriggerYamlDiffDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.metadata.catalog.TriggerCatalogItem;
import io.harness.ngtriggers.beans.source.GitMoveOperationType;
import io.harness.ngtriggers.beans.source.TriggerUpdateCount;
import io.harness.ngtriggers.exceptions.InvalidTriggerYamlException;
import io.harness.ngtriggers.instrumentation.TriggerTelemetryHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.ngtriggers.service.NGTriggerEventsService;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.remote.client.NGRestUtils;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.utils.CryptoUtils;
import io.harness.utils.PageUtils;
import io.harness.utils.PmsFeatureFlagService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import retrofit2.http.Body;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@PipelineServiceAuth
@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class NGTriggerResourceImpl implements NGTriggerResource {
  private final NGTriggerService ngTriggerService;

  private final NGTriggerEventsService ngTriggerEventsService;

  private final NGTriggerEventHistoryResource ngTriggerEventHistoryResource;
  private final NGTriggerElementMapper ngTriggerElementMapper;
  private final AccessControlClient accessControlClient;
  private final NGSettingsClient settingsClient;
  private final FilterService filterService;
  private final PmsFeatureFlagService pmsFeatureFlagService;
  private final TriggerTelemetryHelper triggerTelemetryHelper;

  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<NGTriggerResponseDTO> create(@NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier, @NotNull String yaml, boolean ignoreError,
      boolean withServiceV2) {
    if (getMandatoryPipelineCreateEditPermissionToCreateEditTriggers(
            accountIdentifier, orgIdentifier, projectIdentifier)) {
      accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
          Resource.of("PIPELINE", targetIdentifier), PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT);
    }

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of("PIPELINE", targetIdentifier), PipelineRbacPermissions.PIPELINE_EXECUTE);
    NGTriggerEntity createdEntity = null;
    try {
      TriggerDetails triggerDetails = ngTriggerElementMapper.toTriggerDetails(
          accountIdentifier, orgIdentifier, projectIdentifier, yaml, withServiceV2);
      ngTriggerService.validateTriggerConfig(triggerDetails);

      if (ignoreError) {
        createdEntity = ngTriggerService.create(triggerDetails.getNgTriggerEntity());
      } else {
        ngTriggerService.validatePipelineRef(triggerDetails);
        createdEntity = ngTriggerService.create(triggerDetails.getNgTriggerEntity());
      }
      try {
        triggerTelemetryHelper.sendTriggersCreateEvent(createdEntity, triggerDetails);
      } catch (Exception e) {
        log.error(
            "Error while publishing telemetry for the Triggers Create with id {}.", createdEntity.getIdentifier(), e);
      }

      return ResponseDTO.newResponse(
          createdEntity.getVersion().toString(), ngTriggerElementMapper.toResponseDTO(createdEntity));
    } catch (InvalidTriggerYamlException e) {
      return ResponseDTO.newResponse(ngTriggerElementMapper.toErrorDTO(e));
    } catch (Exception e) {
      throw new InvalidRequestException("Failed while Saving Trigger: " + e.getMessage());
    }
  }

  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<NGTriggerResponseDTO> get(@NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier, String triggerIdentifier) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);

    if (!ngTriggerEntity.isPresent()) {
      throw new EntityNotFoundException(String.format("Trigger %s does not exist", triggerIdentifier));
    }

    return ResponseDTO.newResponse(ngTriggerEntity.get().getVersion().toString(),
        ngTriggerEntity.map(ngTriggerElementMapper::toResponseDTO).orElse(null));
  }

  public ResponseDTO<NGTriggerResponseDTO> update(String ifMatch, @NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier, String triggerIdentifier, @NotNull String yaml,
      boolean ignoreError) {
    if (getMandatoryPipelineCreateEditPermissionToCreateEditTriggers(
            accountIdentifier, orgIdentifier, projectIdentifier)) {
      accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
          Resource.of("PIPELINE", targetIdentifier), PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT);
    }

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of("PIPELINE", targetIdentifier), PipelineRbacPermissions.PIPELINE_EXECUTE);
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);
    if (!ngTriggerEntity.isPresent()) {
      throw new EntityNotFoundException(String.format("Trigger %s does not exist", triggerIdentifier));
    }

    try {
      TriggerDetails triggerDetails = ngTriggerService.fetchTriggerEntity(accountIdentifier, orgIdentifier,
          projectIdentifier, targetIdentifier, triggerIdentifier, yaml, ngTriggerEntity.get().getWithServiceV2());

      ngTriggerService.validateTriggerConfig(triggerDetails);
      triggerDetails.getNgTriggerEntity().setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
      NGTriggerEntity updatedEntity;

      if (ignoreError) {
        updatedEntity = ngTriggerService.update(triggerDetails.getNgTriggerEntity(), ngTriggerEntity.get());
      } else {
        ngTriggerService.validatePipelineRef(triggerDetails);
        updatedEntity = ngTriggerService.update(triggerDetails.getNgTriggerEntity(), ngTriggerEntity.get());
      }
      return ResponseDTO.newResponse(
          updatedEntity.getVersion().toString(), ngTriggerElementMapper.toResponseDTO(updatedEntity));
    } catch (InvalidTriggerYamlException e) {
      return ResponseDTO.newResponse(ngTriggerElementMapper.toErrorDTO(e));
    } catch (Exception e) {
      throw new InvalidRequestException("Failed while updating Trigger: " + e.getMessage());
    }
  }

  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<Boolean> updateTriggerStatus(@NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier, String triggerIdentifier, @NotNull boolean status) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);
    return ResponseDTO.newResponse(ngTriggerService.updateTriggerStatus(ngTriggerEntity.get(), status));
  }

  @Override
  @InternalApi
  public ResponseDTO<Boolean> updateTriggerPollingStatus(
      @NotNull @AccountIdentifier String accountIdentifier, @NotNull PollingTriggerStatusUpdateDTO statusUpdate) {
    return ResponseDTO.newResponse(ngTriggerService.updateTriggerPollingStatus(accountIdentifier, statusUpdate));
  }

  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_EXECUTE)
  public ResponseDTO<Boolean> delete(String ifMatch, @NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier, String triggerIdentifier) {
    boolean triggerDeleted = ngTriggerService.delete(accountIdentifier, orgIdentifier, projectIdentifier,
        targetIdentifier, triggerIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    if (triggerDeleted) {
      ngTriggerEventsService.deleteTriggerEventHistory(
          accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier);
    }
    return ResponseDTO.newResponse(triggerDeleted);
  }

  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<PageResponse<NGTriggerDetailsResponseDTO>> getListForTarget(
      @NotNull @AccountIdentifier String accountIdentifier, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String targetIdentifier,
      String filterQuery, int page, int size, List<String> sort, String searchTerm,
      NGTriggersFilterPropertiesDTO filterProperties) {
    FilterDTO triggerFilterDTO = null;
    if (filterQuery != null) {
      triggerFilterDTO =
          filterService.get(accountIdentifier, orgIdentifier, projectIdentifier, filterQuery, FilterType.TRIGGER);
    }

    Criteria criteria = TriggerFilterHelper.createCriteriaForGetList(accountIdentifier, orgIdentifier,
        projectIdentifier, targetIdentifier, null, searchTerm, false, filterQuery, filterProperties, triggerFilterDTO);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, NGTriggerEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    boolean mandatoryAuth =
        getMandatoryAuthForCustomWebhookTriggers(accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(getNGPageResponse(ngTriggerService.list(criteria, pageRequest).map(triggerEntity -> {
      NGTriggerDetailsResponseDTO responseDTO =
          ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(triggerEntity, true, false, false, mandatoryAuth);
      return responseDTO;
    })));
  }

  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<NGTriggerDetailsResponseDTO> getTriggerDetails(
      @NotNull @AccountIdentifier String accountIdentifier, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, String triggerIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);
    if (!ngTriggerEntity.isPresent()) {
      throw new EntityNotFoundException(String.format(
          "Trigger %s does not exist in project %s in org %s", triggerIdentifier, projectIdentifier, orgIdentifier));
    }
    return ResponseDTO.newResponse(ngTriggerEntity.get().getVersion().toString(),
        ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity.get(), true, true, false,
            getMandatoryAuthForCustomWebhookTriggers(accountIdentifier, orgIdentifier, projectIdentifier)));
  }

  @Timed
  @ExceptionMetered
  public RestResponse<String> generateWebhookToken() {
    return new RestResponse<>(CryptoUtils.secureRandAlphaNumString(40));
  }

  @Override
  public ResponseDTO<NGTriggerCatalogDTO> getTriggerCatalog(String accountIdentifier) {
    List<TriggerCatalogItem> triggerCatalog = ngTriggerService.getTriggerCatalog(accountIdentifier);
    return ResponseDTO.newResponse(ngTriggerElementMapper.toCatalogDTO(triggerCatalog));
  }

  @Override
  public ResponseDTO<Page<NGTriggerEventHistoryDTO>> getTriggerEventHistory(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String targetIdentifier, String triggerIdentifier,
      String searchTerm, int page, int size, List<String> sort) {
    return ngTriggerEventHistoryResource.getTriggerEventHistory(accountIdentifier, orgIdentifier, projectIdentifier,
        targetIdentifier, triggerIdentifier, searchTerm, page, size, sort);
  }

  @Override
  public ResponseDTO<TriggerYamlDiffDTO> getTriggerReconciliationYamlDiff(
      @NotNull @AccountIdentifier String accountIdentifier, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String targetIdentifier,
      String triggerIdentifier) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);
    if (!ngTriggerEntity.isPresent()) {
      throw new EntityNotFoundException(String.format("Trigger %s does not exist", triggerIdentifier));
    }
    TriggerDetails triggerDetails =
        ngTriggerService.fetchTriggerEntity(accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier,
            triggerIdentifier, ngTriggerEntity.get().getYaml(), ngTriggerEntity.get().getWithServiceV2());
    return ResponseDTO.newResponse(ngTriggerService.getTriggerYamlDiff(triggerDetails));
  }

  @Override
  @Hidden
  public ResponseDTO<NGTriggerConfigV2> getNGTriggerConfigV2() {
    return null;
  }

  @Override
  @InternalApi
  public ResponseDTO<TriggerUpdateCount> updateBranchName(@NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String targetIdentifier, GitMoveOperationType operationType,
      String pipelineBranchName) {
    return ResponseDTO.newResponse(ngTriggerService.updateBranchName(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, operationType, pipelineBranchName));
  }

  private boolean getMandatoryAuthForCustomWebhookTriggers(
      String accountId, String orgIdentifier, String projectIdentifier) {
    boolean mandatoryAuth =
        Objects.equals(NGRestUtils
                           .getResponse(settingsClient.getSetting(
                               MANDATE_CUSTOM_WEBHOOK_AUTHORIZATION, accountId, orgIdentifier, projectIdentifier))
                           .getValue(),
            "true");

    return mandatoryAuth;
  }

  private boolean getMandatoryPipelineCreateEditPermissionToCreateEditTriggers(
      String accountId, String orgIdentifier, String projectIdentifier) {
    String response;
    try {
      response =
          NGRestUtils
              .getResponse(settingsClient.getSetting(MANDATE_PIPELINE_CREATE_EDIT_PERMISSION_TO_CREATE_EDIT_TRIGGERS,
                  accountId, orgIdentifier, projectIdentifier))
              .getValue();
    } catch (Exception ex) {
      log.error("Failed to fetch setting {} for accountId {} orgId {} and projectId {}",
          MANDATE_PIPELINE_CREATE_EDIT_PERMISSION_TO_CREATE_EDIT_TRIGGERS, accountId, orgIdentifier, projectIdentifier,
          ex);
      return true;
    }
    return Objects.equals(response, "true");
  }

  public ResponseDTO<BulkTriggersResponseDTO> bulkToggleTriggers(@NotNull @AccountIdentifier String accountIdentifier,
      @NotNull @Body BulkTriggersRequestDTO bulkTriggersRequestDTO) {
    long timeStart = System.currentTimeMillis();

    BulkTriggersResponseDTO bulkTriggersResponseDTO =
        ngTriggerService.toggleTriggersInBulk(accountIdentifier, bulkTriggersRequestDTO);

    long timeTaken = System.currentTimeMillis() - timeStart;

    try {
      triggerTelemetryHelper.sendBulkToggleTriggersApiEvent(
          accountIdentifier, bulkTriggersRequestDTO, bulkTriggersResponseDTO, timeTaken);
    } catch (Exception e) {
      log.error("Error while publishing telemetry for the Bulk Toggle Triggers API.");
    }

    return ResponseDTO.newResponse(bulkTriggersResponseDTO);
  }
}
