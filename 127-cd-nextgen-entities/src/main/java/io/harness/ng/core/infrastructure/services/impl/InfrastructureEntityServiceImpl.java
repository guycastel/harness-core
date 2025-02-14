/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.infrastructure.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.pms.pipeline.MoveConfigOperationType.INLINE_TO_REMOTE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.IDENTIFIER;
import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;
import static io.harness.utils.IdentifierRefHelper.MAX_RESULT_THRESHOLD_FOR_SPLIT;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.EntityType;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.customdeployment.helper.CustomDeploymentEntitySetupHelper;
import io.harness.cdng.infra.mapper.InfrastructureEntityConfigMapper;
import io.harness.cdng.infra.yaml.InfrastructureConfig;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InternalServerErrorException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ScmException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnsupportedOperationException;
import io.harness.exception.WingsException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitaware.helper.GitAwareEntityHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitx.EntityGitDetailsGuard;
import io.harness.gitx.GitXSettingsHelper;
import io.harness.ng.DuplicateKeyExceptionParser;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.events.EnvironmentUpdatedEvent;
import io.harness.ng.core.infrastructure.InfrastructureType;
import io.harness.ng.core.infrastructure.dto.InfraMoveConfigOperationDTO;
import io.harness.ng.core.infrastructure.dto.InfraMoveConfigResponse;
import io.harness.ng.core.infrastructure.dto.InfrastructureInputsMergedResponseDto;
import io.harness.ng.core.infrastructure.dto.InfrastructureYamlMetadata;
import io.harness.ng.core.infrastructure.dto.NoInputMergeInputAction;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity.InfrastructureEntityKeys;
import io.harness.ng.core.infrastructure.services.InfrastructureEntityService;
import io.harness.ng.core.service.services.impl.InputSetMergeUtility;
import io.harness.ng.core.serviceoverridev2.service.ServiceOverridesServiceV2;
import io.harness.ng.core.utils.CDGitXService;
import io.harness.ng.core.utils.GitXUtils;
import io.harness.ng.core.utils.ServiceOverrideV2ValidationHelper;
import io.harness.outbox.api.OutboxService;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.pms.merger.helpers.RuntimeInputFormHelper;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.UpsertOptions;
import io.harness.repositories.infrastructure.spring.InfrastructureRepository;
import io.harness.setupusage.InfrastructureEntitySetupUsageHelper;
import io.harness.utils.ExceptionCreationUtils;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.NGFeatureFlagHelperService;
import io.harness.utils.YamlPipelineUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InfrastructureEntityServiceImpl implements InfrastructureEntityService {
  private final InfrastructureRepository infrastructureRepository;
  @Inject @Named(OUTBOX_TRANSACTION_TEMPLATE) private final TransactionTemplate transactionTemplate;
  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_RETRY_POLICY;
  private final OutboxService outboxService;
  @Inject CustomDeploymentEntitySetupHelper customDeploymentEntitySetupHelper;
  @Inject private InfrastructureEntitySetupUsageHelper infrastructureEntitySetupUsageHelper;

  @Inject private HPersistence hPersistence;
  @Inject private ServiceOverridesServiceV2 serviceOverridesServiceV2;
  @Inject private ServiceOverrideV2ValidationHelper overrideV2ValidationHelper;
  @Inject @Named("environment-gitx-executor") private ExecutorService executorService;
  @Inject private EnvironmentService environmentService;
  private final GitAwareEntityHelper gitAwareEntityHelper;
  @Inject private CDGitXService cdGitXService;
  @Inject private GitXSettingsHelper gitXSettingsHelper;
  @Inject private NGFeatureFlagHelperService ngFeatureFlagHelperService;

  private static final String DUP_KEY_EXP_FORMAT_STRING_FOR_PROJECT =
      "Infrastructure [%s] under Environment [%s] Project[%s], Organization [%s] in Account [%s] already exists";
  private static final String DUP_KEY_EXP_FORMAT_STRING_FOR_ORG =
      "Infrastructure [%s] under Organization [%s] in Account [%s] already exists";
  private static final String DUP_KEY_EXP_FORMAT_STRING_FOR_ACCOUNT =
      "Infrastructure [%s] in Account [%s] already exists";
  private static final int REMOTE_INFRASTRUCTURES_BATCH_SIZE = 20;

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public InfrastructureEntity create(@NotNull @Valid InfrastructureEntity infraEntity) {
    try {
      setObsoleteAsFalse(infraEntity);
      validatePresenceOfRequiredFields(
          infraEntity.getAccountId(), infraEntity.getIdentifier(), infraEntity.getEnvIdentifier());
      setNameIfNotPresent(infraEntity);
      modifyInfraRequest(infraEntity);
      Set<EntityDetailProtoDTO> referredEntities = getAndValidateReferredEntities(infraEntity);
      InfrastructureEntity createdInfra =
          Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
            InfrastructureEntity infrastructureEntity = infrastructureRepository.saveGitAware(infraEntity);
            outboxService.save(EnvironmentUpdatedEvent.builder()
                                   .accountIdentifier(infraEntity.getAccountId())
                                   .orgIdentifier(infraEntity.getOrgIdentifier())
                                   .status(EnvironmentUpdatedEvent.Status.CREATED)
                                   .resourceType(EnvironmentUpdatedEvent.ResourceType.INFRASTRUCTURE)
                                   .projectIdentifier(infraEntity.getProjectIdentifier())
                                   .newInfrastructureEntity(infraEntity)
                                   .build());
            return infrastructureEntity;
          }));
      infrastructureEntitySetupUsageHelper.createSetupUsages(createdInfra, referredEntities);
      if (infraEntity.getType() == InfrastructureType.CUSTOM_DEPLOYMENT) {
        customDeploymentEntitySetupHelper.addReferencesInEntitySetupUsage(infraEntity);
      }
      return createdInfra;

    } catch (ExplanationException | HintException | ScmException ex) {
      log.error(String.format("Error while saving infrastructure: [%s]", infraEntity.getIdentifier()), ex);
      throw ex;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          getDuplicateInfrastructureExistsErrorMessage(infraEntity.getAccountId(), infraEntity.getOrgIdentifier(),
              infraEntity.getProjectIdentifier(), infraEntity.getEnvIdentifier(), infraEntity.getIdentifier()),
          USER, ex);
    }
  }

  private Set<EntityDetailProtoDTO> getAndValidateReferredEntities(InfrastructureEntity infraEntity) {
    try {
      return infrastructureEntitySetupUsageHelper.getAllReferredEntities(infraEntity);
    } catch (RuntimeException ex) {
      throw new InvalidRequestException(
          String.format(
              "Exception while retrieving referred entities for infrastructure: [%s]. ", infraEntity.getIdentifier())
          + ex.getMessage());
    }
  }

  @Override
  public Optional<InfrastructureEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentRef, String infraIdentifier) {
    checkArgument(isNotEmpty(accountId), "accountId must be present");

    return getInfrastructureByRef(
        accountId, orgIdentifier, projectIdentifier, environmentRef, infraIdentifier, false, false, false);
  }

  @Override
  public Optional<InfrastructureEntity> getMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentRef, String infraIdentifier) {
    checkArgument(isNotEmpty(accountId), "accountId must be present");

    return getInfrastructureByRef(
        accountId, orgIdentifier, projectIdentifier, environmentRef, infraIdentifier, false, false, true);
  }

  @Override
  public Optional<InfrastructureEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentRef, String infraIdentifier, boolean loadFromCache, boolean loadFromFallbackBranch) {
    checkArgument(isNotEmpty(accountId), "accountId must be present");

    return getInfrastructureByRef(accountId, orgIdentifier, projectIdentifier, environmentRef, infraIdentifier,
        loadFromCache, loadFromFallbackBranch, false);
  }

  private Optional<InfrastructureEntity> getInfrastructureByRef(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String infraIdentifier, boolean loadFromCache,
      boolean loadFromFallbackBranch, boolean getMetadataOnly) {
    // get using environmentRef
    String[] envRefSplit = StringUtils.split(environmentRef, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return infrastructureRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnvIdentifierAndIdentifier(
          accountId, orgIdentifier, projectIdentifier, environmentRef, infraIdentifier, loadFromCache,
          loadFromFallbackBranch, getMetadataOnly);
    } else {
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(environmentRef, accountId, orgIdentifier, projectIdentifier);
      return infrastructureRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnvIdentifierAndIdentifier(
          envIdentifierRef.getAccountIdentifier(), envIdentifierRef.getOrgIdentifier(),
          envIdentifierRef.getProjectIdentifier(), envIdentifierRef.getIdentifier(), infraIdentifier, loadFromCache,
          loadFromFallbackBranch, getMetadataOnly);
    }
  }

  @Override
  public InfrastructureEntity update(@Valid InfrastructureEntity requestInfra) {
    validatePresenceOfRequiredFields(requestInfra.getAccountId(), requestInfra.getIdentifier());
    setObsoleteAsFalse(requestInfra);
    setNameIfNotPresent(requestInfra);
    modifyInfraRequest(requestInfra);
    Set<EntityDetailProtoDTO> referredEntities = getAndValidateReferredEntities(requestInfra);
    Criteria criteria = getInfrastructureEqualityCriteria(requestInfra);
    Optional<InfrastructureEntity> infraEntityOptional =
        get(requestInfra.getAccountId(), requestInfra.getOrgIdentifier(), requestInfra.getProjectIdentifier(),
            requestInfra.getEnvIdentifier(), requestInfra.getIdentifier());
    if (infraEntityOptional.isPresent()) {
      InfrastructureEntity oldInfrastructureEntity = infraEntityOptional.get();
      validateImmutableFieldsAndThrow(requestInfra, oldInfrastructureEntity);
      InfrastructureEntity infraToUpdate = oldInfrastructureEntity.withYaml(requestInfra.getYaml())
                                               .withDescription(requestInfra.getDescription())
                                               .withName(requestInfra.getName())
                                               .withTags(requestInfra.getTags())
                                               .withType(requestInfra.getType());

      InfrastructureEntity updatedInfra =
          Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
            InfrastructureEntity updatedResult = infrastructureRepository.update(criteria, infraToUpdate);
            if (updatedResult == null) {
              throw new InvalidRequestException(format(
                  "Infrastructure [%s] under Environment [%s], Project [%s], Organization [%s] couldn't be updated or doesn't exist.",
                  requestInfra.getIdentifier(), requestInfra.getEnvIdentifier(), requestInfra.getProjectIdentifier(),
                  requestInfra.getOrgIdentifier()));
            }
            outboxService.save(EnvironmentUpdatedEvent.builder()
                                   .accountIdentifier(requestInfra.getAccountId())
                                   .orgIdentifier(requestInfra.getOrgIdentifier())
                                   .status(EnvironmentUpdatedEvent.Status.UPDATED)
                                   .resourceType(EnvironmentUpdatedEvent.ResourceType.INFRASTRUCTURE)
                                   .projectIdentifier(requestInfra.getProjectIdentifier())
                                   .newInfrastructureEntity(requestInfra)
                                   .oldInfrastructureEntity(infraEntityOptional.get())
                                   .build());
            return updatedResult;
          }));
      infrastructureEntitySetupUsageHelper.updateSetupUsages(updatedInfra, referredEntities);
      if (requestInfra.getType() == InfrastructureType.CUSTOM_DEPLOYMENT) {
        customDeploymentEntitySetupHelper.addReferencesInEntitySetupUsage(requestInfra);
      }
      return updatedInfra;
    } else {
      throw new InvalidRequestException(
          format("Infrastructure [%s] under Environment [%s], Project [%s], Organization [%s] doesn't exist.",
              requestInfra.getIdentifier(), requestInfra.getEnvIdentifier(), requestInfra.getProjectIdentifier(),
              requestInfra.getOrgIdentifier()));
    }
  }

  private void validateImmutableFieldsAndThrow(InfrastructureEntity requestInfra, InfrastructureEntity oldInfra) {
    if (oldInfra != null) {
      if (oldInfra.getDeploymentType() != null && requestInfra.getDeploymentType() != null
          && !oldInfra.getDeploymentType().equals(requestInfra.getDeploymentType())) {
        throw new InvalidRequestException("Infrastructure Deployment Type is not allowed to change.");
      }
      if (oldInfra.getType() != null && requestInfra.getType() != null
          && !oldInfra.getType().equals(requestInfra.getType())) {
        throw new InvalidRequestException("Infrastructure Type is not allowed to change.");
      }
    }
  }

  @Override
  public InfrastructureEntity upsert(@Valid InfrastructureEntity requestInfra, UpsertOptions upsertOptions) {
    validatePresenceOfRequiredFields(requestInfra.getAccountId(), requestInfra.getIdentifier());
    setNameIfNotPresent(requestInfra);
    modifyInfraRequest(requestInfra);
    Set<EntityDetailProtoDTO> referredEntities = getAndValidateReferredEntities(requestInfra);
    Criteria criteria = getInfrastructureEqualityCriteria(requestInfra);

    Optional<InfrastructureEntity> infraEntityOptional =
        get(requestInfra.getAccountId(), requestInfra.getOrgIdentifier(), requestInfra.getProjectIdentifier(),
            requestInfra.getEnvIdentifier(), requestInfra.getIdentifier());
    if (infraEntityOptional.isPresent()) {
      InfrastructureEntity oldInfrastructureEntity = infraEntityOptional.get();

      if (oldInfrastructureEntity != null && oldInfrastructureEntity.getDeploymentType() != null
          && requestInfra.getDeploymentType() != null
          && !oldInfrastructureEntity.getDeploymentType().equals(requestInfra.getDeploymentType())) {
        throw new InvalidRequestException(String.format("Infrastructure Deployment Type is not allowed to change."));
      }
    }

    InfrastructureEntity upsertedInfra =
        Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
          InfrastructureEntity result = infrastructureRepository.upsert(criteria, requestInfra);
          if (result == null) {
            throw new InvalidRequestException(String.format(
                "Infrastructure [%s] under Environment [%s] Project[%s], Organization [%s] couldn't be upserted.",
                requestInfra.getIdentifier(), requestInfra.getEnvIdentifier(), requestInfra.getProjectIdentifier(),
                requestInfra.getOrgIdentifier()));
          }
          outboxService.save(EnvironmentUpdatedEvent.builder()
                                 .accountIdentifier(requestInfra.getAccountId())
                                 .orgIdentifier(requestInfra.getOrgIdentifier())
                                 .status(EnvironmentUpdatedEvent.Status.UPSERTED)
                                 .resourceType(EnvironmentUpdatedEvent.ResourceType.INFRASTRUCTURE)
                                 .projectIdentifier(requestInfra.getProjectIdentifier())
                                 .newInfrastructureEntity(requestInfra)
                                 .build());
          return result;
        }));
    infrastructureEntitySetupUsageHelper.updateSetupUsages(upsertedInfra, referredEntities);
    if (requestInfra.getType() == InfrastructureType.CUSTOM_DEPLOYMENT) {
      customDeploymentEntitySetupHelper.addReferencesInEntitySetupUsage(requestInfra);
    }
    return upsertedInfra;
  }

  @Override
  public Page<InfrastructureEntity> list(@NotNull Criteria criteria, @NotNull Pageable pageable) {
    return infrastructureRepository.findAll(criteria, pageable);
  }

  @Override
  public HIterator<InfrastructureEntity> listIterator(
      String accountId, String orgIdentifier, String projectIdentifier, String envRef, Collection<String> identifiers) {
    checkArgument(isNotEmpty(accountId), "account id must be present");

    String[] envRefSplit = StringUtils.split(envRef, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return new HIterator<>(hPersistence.createQuery(InfrastructureEntity.class)
                                 .filter(InfrastructureEntityKeys.accountId, accountId)
                                 .filter(InfrastructureEntityKeys.orgIdentifier, orgIdentifier)
                                 .filter(InfrastructureEntityKeys.projectIdentifier, projectIdentifier)
                                 .filter(InfrastructureEntityKeys.envIdentifier, envRef)
                                 .field(InfrastructureEntityKeys.identifier)
                                 .in(identifiers)
                                 .fetch());
    } else {
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(envRef, accountId, orgIdentifier, projectIdentifier);
      return new HIterator<>(
          hPersistence.createQuery(InfrastructureEntity.class)
              .filter(InfrastructureEntityKeys.accountId, envIdentifierRef.getAccountIdentifier())
              .filter(InfrastructureEntityKeys.orgIdentifier, envIdentifierRef.getOrgIdentifier())
              .filter(InfrastructureEntityKeys.projectIdentifier, envIdentifierRef.getProjectIdentifier())
              .filter(InfrastructureEntityKeys.envIdentifier, envIdentifierRef.getIdentifier())
              .field(InfrastructureEntityKeys.identifier)
              .in(identifiers)
              .fetch());
    }
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String envRef,
      String infraIdentifier, boolean forceDelete) {
    InfrastructureEntity infraEntity = InfrastructureEntity.builder()
                                           .accountId(accountId)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .envIdentifier(envRef)
                                           .identifier(infraIdentifier)
                                           .build();

    if (!forceDelete) {
      infrastructureEntitySetupUsageHelper.checkThatInfraIsNotReferredByOthers(infraEntity);
    }

    Criteria criteria = getInfrastructureEqualityCriteria(infraEntity);
    Optional<InfrastructureEntity> infraEntityOptional =
        getMetadata(accountId, orgIdentifier, projectIdentifier, envRef, infraIdentifier);

    if (infraEntityOptional.isPresent()) {
      if (infraEntityOptional.get().getType() == InfrastructureType.CUSTOM_DEPLOYMENT) {
        customDeploymentEntitySetupHelper.deleteReferencesInEntitySetupUsage(infraEntityOptional.get());
      }
      boolean infraDeleteResult =
          Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
            DeleteResult deleteResult = infrastructureRepository.delete(criteria);
            if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() != 1) {
              throw new InvalidRequestException(String.format(
                  "Infrastructure [%s] under Environment [%s], Project[%s], Organization [%s] couldn't be deleted.",
                  infraIdentifier, envRef, projectIdentifier, orgIdentifier));
            }

            infraEntityOptional.ifPresent(
                infrastructureEntity -> infrastructureEntitySetupUsageHelper.deleteSetupUsages(infrastructureEntity));

            outboxService.save(EnvironmentUpdatedEvent.builder()
                                   .accountIdentifier(accountId)
                                   .orgIdentifier(orgIdentifier)
                                   .projectIdentifier(projectIdentifier)
                                   .oldInfrastructureEntity(infraEntityOptional.get())
                                   .status(forceDelete ? EnvironmentUpdatedEvent.Status.FORCE_DELETED
                                                       : EnvironmentUpdatedEvent.Status.DELETED)
                                   .resourceType(EnvironmentUpdatedEvent.ResourceType.INFRASTRUCTURE)
                                   .build());
            return true;
          }));
      if (overrideV2ValidationHelper.isOverridesV2Enabled(accountId, orgIdentifier, projectIdentifier)) {
        processDownstreamDeletions(accountId, orgIdentifier, projectIdentifier, envRef, infraIdentifier);
      }
      return infraDeleteResult;
    } else {
      throw new InvalidRequestException(
          String.format("Infrastructure [%s] under Environment [%s], Project[%s], Organization [%s] doesn't exist.",
              infraIdentifier, envRef, projectIdentifier, orgIdentifier));
    }
  }

  @Override
  public boolean forceDeleteAllInEnv(String accountId, String orgIdentifier, String projectIdentifier, String envRef) {
    checkArgument(isNotEmpty(accountId), "account id must be present");
    checkArgument(isNotEmpty(envRef), "env identifier must be present");

    Criteria criteria = getInfrastructureEqualityCriteriaForEnv(accountId, orgIdentifier, projectIdentifier, envRef);

    List<InfrastructureEntity> infrastructureEntityListForEnvIdentifier =
        getAllInfrastructureMetadataFromEnvRef(accountId, orgIdentifier, projectIdentifier, envRef);

    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      DeleteResult deleteResult = infrastructureRepository.delete(criteria);

      if (deleteResult.wasAcknowledged()) {
        for (InfrastructureEntity infra : infrastructureEntityListForEnvIdentifier) {
          infrastructureEntitySetupUsageHelper.deleteSetupUsages(infra);
        }
      } else {
        log.error(
            String.format("Infrastructures under Environment [%s], Project[%s], Organization [%s] couldn't be deleted.",
                envRef, projectIdentifier, orgIdentifier));
      }

      return deleteResult.wasAcknowledged();
    }));
  }

  @Override
  public boolean forceDeleteAllInProject(String accountId, String orgIdentifier, String projectIdentifier) {
    checkArgument(isNotEmpty(accountId), "account id must be present");
    checkArgument(isNotEmpty(orgIdentifier), "org id must be present");
    checkArgument(isNotEmpty(projectIdentifier), "project id must be present");

    return forceDeleteAllInternal(accountId, orgIdentifier, projectIdentifier);
  }

  @Override
  public boolean forceDeleteAllInOrg(String accountId, String orgIdentifier) {
    checkArgument(isNotEmpty(accountId), "account id must be present");
    checkArgument(isNotEmpty(orgIdentifier), "org identifier must be present");

    return forceDeleteAllInternal(accountId, orgIdentifier, null);
  }

  private boolean forceDeleteAllInternal(String accountId, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = getInfrastructureEqualityCriteria(accountId, orgIdentifier, projectIdentifier);
    List<InfrastructureEntity> infrastructureEntityList =
        getInfrastructures(accountId, orgIdentifier, projectIdentifier);

    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      DeleteResult deleteResult = infrastructureRepository.delete(criteria);

      if (deleteResult.wasAcknowledged()) {
        for (InfrastructureEntity infra : infrastructureEntityList) {
          infrastructureEntitySetupUsageHelper.deleteSetupUsages(infra);
        }
      } else {
        log.error(getScopedErrorForCascadeDeletion(orgIdentifier, projectIdentifier));
      }

      return deleteResult.wasAcknowledged();
    }));
  }

  private String getScopedErrorForCascadeDeletion(String orgIdentifier, String projectIdentifier) {
    if (isNotEmpty(projectIdentifier)) {
      return String.format("Infrastructures under Project[%s], Organization [%s] couldn't be deleted.",
          projectIdentifier, orgIdentifier);
    }
    return String.format("Infrastructures under Organization: [%s] couldn't be deleted.", orgIdentifier);
  }

  private void setObsoleteAsFalse(InfrastructureEntity requestInfra) {
    requestInfra.setObsolete(false);
  }
  private void setNameIfNotPresent(InfrastructureEntity requestInfra) {
    if (isEmpty(requestInfra.getName())) {
      requestInfra.setName(requestInfra.getIdentifier());
    }
  }
  private Criteria getInfrastructureEqualityCriteria(@Valid InfrastructureEntity requestInfra) {
    checkArgument(isNotEmpty(requestInfra.getAccountId()), "accountId must be present");

    // infra id will be provided
    String[] envRefSplit = StringUtils.split(requestInfra.getEnvIdentifier(), ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return Criteria.where(InfrastructureEntityKeys.accountId)
          .is(requestInfra.getAccountId())
          .and(InfrastructureEntityKeys.orgIdentifier)
          .is(requestInfra.getOrgIdentifier())
          .and(InfrastructureEntityKeys.projectIdentifier)
          .is(requestInfra.getProjectIdentifier())
          .and(InfrastructureEntityKeys.envIdentifier)
          .is(requestInfra.getEnvIdentifier())
          .and(InfrastructureEntityKeys.identifier)
          .is(requestInfra.getIdentifier());
    } else {
      IdentifierRef envIdentifierRef = IdentifierRefHelper.getIdentifierRef(requestInfra.getEnvIdentifier(),
          requestInfra.getAccountId(), requestInfra.getOrgIdentifier(), requestInfra.getProjectIdentifier());
      return Criteria.where(InfrastructureEntityKeys.accountId)
          .is(envIdentifierRef.getAccountIdentifier())
          .and(InfrastructureEntityKeys.orgIdentifier)
          .is(envIdentifierRef.getOrgIdentifier())
          .and(InfrastructureEntityKeys.projectIdentifier)
          .is(envIdentifierRef.getProjectIdentifier())
          .and(InfrastructureEntityKeys.envIdentifier)
          .is(envIdentifierRef.getIdentifier())
          .and(InfrastructureEntityKeys.identifier)
          .is(requestInfra.getIdentifier());
    }
  }

  @Override
  public Page<InfrastructureEntity> bulkCreate(String accountId, @NotNull List<InfrastructureEntity> infraEntities) {
    try {
      validateInfraList(infraEntities);
      populateDefaultNameIfNotPresent(infraEntities);
      modifyInfraRequestBatch(infraEntities);
      List<Set<EntityDetailProtoDTO>> referredEntityList = new ArrayList<>();
      for (InfrastructureEntity infrastructureEntity : infraEntities) {
        referredEntityList.add(getAndValidateReferredEntities(infrastructureEntity));
      }
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        List<InfrastructureEntity> outputInfrastructureEntitiesList =
            (List<InfrastructureEntity>) infrastructureRepository.saveAll(infraEntities);
        int i = 0;
        for (InfrastructureEntity infraEntity : infraEntities) {
          outboxService.save(EnvironmentUpdatedEvent.builder()
                                 .accountIdentifier(infraEntity.getAccountId())
                                 .orgIdentifier(infraEntity.getOrgIdentifier())
                                 .status(EnvironmentUpdatedEvent.Status.CREATED)
                                 .resourceType(EnvironmentUpdatedEvent.ResourceType.INFRASTRUCTURE)
                                 .projectIdentifier(infraEntity.getProjectIdentifier())
                                 .newInfrastructureEntity(infraEntity)
                                 .build());
          infrastructureEntitySetupUsageHelper.createSetupUsages(infraEntity, referredEntityList.get(i));
          i++;
        }

        return new PageImpl<>(outputInfrastructureEntitiesList);
      }));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          getDuplicateInfrastructureExistsErrorMessage(accountId, ex.getMessage()), USER, ex);
    } catch (WingsException ex) {
      String infraNames = infraEntities.stream().map(InfrastructureEntity::getName).collect(Collectors.joining(","));
      log.info("Encountered exception while saving the infrastructure entity records of [{}], with exception",
          infraNames, ex);
      throw new InvalidRequestException(
          "Encountered exception while saving the infrastructure entity records. " + ex.getMessage());
    } catch (Exception ex) {
      String infraNames = infraEntities.stream().map(InfrastructureEntity::getName).collect(Collectors.joining(","));
      log.info("Encountered exception while saving the infrastructure entity records of [{}], with exception",
          infraNames, ex);
      throw new UnexpectedException("Encountered exception while saving the infrastructure entity records.");
    }
  }

  @Override
  public List<InfrastructureEntity> getAllInfrastructureMetadataFromIdentifierList(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String envIdentifier, List<String> infraIdentifierList) {
    String[] envRefSplit = StringUtils.split(envIdentifier, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return infrastructureRepository.findAllFromInfraIdentifierList(
          accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier, infraIdentifierList);
    } else {
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(envIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
      return infrastructureRepository.findAllFromInfraIdentifierList(envIdentifierRef.getAccountIdentifier(),
          envIdentifierRef.getOrgIdentifier(), envIdentifierRef.getProjectIdentifier(),
          envIdentifierRef.getIdentifier(), infraIdentifierList);
    }
  }

  private List<InfrastructureEntity> getAllInfrastructuresWithYamlCommon(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String environmentBranch,
      List<InfrastructureEntity> infrastructureEntities) {
    GitEntityInfo gitContextForInfra = getGitDetailsForInfrastructure(
        accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier, environmentBranch);

    try (EntityGitDetailsGuard ignore = new EntityGitDetailsGuard(gitContextForInfra)) {
      populateInfrastructuresWithYaml(infrastructureEntities, false);
    } catch (CompletionException ex) {
      // internal method always wraps the CompletionException, so we will have a cause
      log.error(String.format("Error while getting infrastructure YAML for env ref: %s", envIdentifier), ex);
      Throwables.throwIfUnchecked(ex.getCause());
    } catch (Exception ex) {
      log.error(
          String.format("Unexpected error occurred while getting infrastructure YAML for env ref: %s", envIdentifier),
          ex);
      throw new InternalServerErrorException(
          String.format("Unexpected error occurred while getting infrastructure YAML for env ref: %s: [%s]",
              envIdentifier, ex.getMessage()),
          ex);
    }

    return infrastructureEntities;
  }

  @Override
  public List<InfrastructureEntity> getAllInfrastructuresWithYamlFromIdentifierList(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String envIdentifier, String environmentBranch,
      List<String> infraIdentifierList) {
    List<InfrastructureEntity> entities = getAllInfrastructureMetadataFromIdentifierList(
        accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier, infraIdentifierList);

    return getAllInfrastructuresWithYamlCommon(
        accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier, environmentBranch, entities);
  }

  @Override
  public List<InfrastructureEntity> getAllInfrastructureMetadataFromEnvRef(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String envRef) {
    String[] envRefSplit = StringUtils.split(envRef, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return infrastructureRepository.findAllFromEnvIdentifier(
          accountIdentifier, orgIdentifier, projectIdentifier, envRef);
    } else {
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(envRef, accountIdentifier, orgIdentifier, projectIdentifier);
      return infrastructureRepository.findAllFromEnvIdentifier(envIdentifierRef.getAccountIdentifier(),
          envIdentifierRef.getOrgIdentifier(), envIdentifierRef.getProjectIdentifier(),
          envIdentifierRef.getIdentifier());
    }
  }

  @Override
  public List<InfrastructureEntity> getAllInfrastructuresWithYamlFromEnvRef(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String envRef, String environmentBranch) {
    List<InfrastructureEntity> entities =
        getAllInfrastructureMetadataFromEnvRef(accountIdentifier, orgIdentifier, projectIdentifier, envRef);

    return getAllInfrastructuresWithYamlCommon(
        accountIdentifier, orgIdentifier, projectIdentifier, envRef, environmentBranch, entities);
  }

  @Override
  public List<InfrastructureEntity> getAllInfrastructureFromEnvRefAndDeploymentType(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String envRef, ServiceDefinitionType deploymentType) {
    String[] envRefSplit = StringUtils.split(envRef, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return infrastructureRepository.findAllFromEnvIdentifierAndDeploymentType(
          accountIdentifier, orgIdentifier, projectIdentifier, envRef, deploymentType);
    } else {
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(envRef, accountIdentifier, orgIdentifier, projectIdentifier);
      return infrastructureRepository.findAllFromEnvIdentifierAndDeploymentType(envIdentifierRef.getAccountIdentifier(),
          envIdentifierRef.getOrgIdentifier(), envIdentifierRef.getProjectIdentifier(),
          envIdentifierRef.getIdentifier(), deploymentType);
    }
  }

  @Override
  public List<InfrastructureEntity> getInfrastructures(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return infrastructureRepository.findAllFromProjectIdentifier(accountIdentifier, orgIdentifier, projectIdentifier);
  }
  @Override
  public String createInfrastructureInputsFromYaml(String accountId, String orgIdentifier, String projectIdentifier,
      String envRef, String environmentBranch, List<String> infraIdentifiers, boolean deployToAll,
      NoInputMergeInputAction noInputMergeInputAction) {
    Map<String, Object> yamlInputs = createInfrastructureInputsYamlInternal(accountId, orgIdentifier, projectIdentifier,
        envRef, environmentBranch, deployToAll, infraIdentifiers, noInputMergeInputAction);

    if (isEmpty(yamlInputs)) {
      return null;
    }
    return YamlPipelineUtils.writeYamlString(yamlInputs);
  }

  @Override
  public UpdateResult batchUpdateInfrastructure(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, List<String> infraIdentifierList, Update update) {
    String[] envRefSplit = StringUtils.split(envIdentifier, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return infrastructureRepository.batchUpdateInfrastructure(
          accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier, infraIdentifierList, update);
    } else {
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(envIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
      return infrastructureRepository.batchUpdateInfrastructure(envIdentifierRef.getAccountIdentifier(),
          envIdentifierRef.getOrgIdentifier(), envIdentifierRef.getProjectIdentifier(),
          envIdentifierRef.getIdentifier(), infraIdentifierList, update);
    }
  }

  private Map<String, Object> createInfrastructureInputsYamlInternal(String accountId, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String environmentBranch, boolean deployToAll,
      List<String> infraIdentifiers, NoInputMergeInputAction noInputMergeInputAction) {
    Map<String, Object> yamlInputs = new HashMap<>();
    List<ObjectNode> infraDefinitionInputList = new ArrayList<>();
    // create one mapper for all infra defs
    ObjectMapper mapper = new ObjectMapper();

    List<InfrastructureEntity> infrastructureEntities;
    if (deployToAll) {
      infrastructureEntities = getAllInfrastructuresWithYamlFromEnvRef(
          accountId, orgIdentifier, projectIdentifier, envIdentifier, environmentBranch);
    } else {
      infrastructureEntities = getAllInfrastructuresWithYamlFromIdentifierList(
          accountId, orgIdentifier, projectIdentifier, envIdentifier, environmentBranch, infraIdentifiers);
    }

    for (InfrastructureEntity infraEntity : infrastructureEntities) {
      Optional<ObjectNode> infraDefinitionNodeWithInputsOptional =
          createInfraDefinitionNodeWithInputs(infraEntity, mapper);
      if (infraDefinitionNodeWithInputsOptional.isPresent()) {
        infraDefinitionInputList.add(infraDefinitionNodeWithInputsOptional.get());
      } else if (noInputMergeInputAction.equals(NoInputMergeInputAction.ADD_IDENTIFIER_NODE)) {
        ObjectNode infraNode = mapper.createObjectNode();
        infraNode.put(IDENTIFIER, infraEntity.getIdentifier());
        infraDefinitionInputList.add(infraNode);
      }
    }

    if (isNotEmpty(infraDefinitionInputList)) {
      yamlInputs.put(YamlTypes.INFRASTRUCTURE_DEFS, infraDefinitionInputList);
    }
    return yamlInputs;
  }

  /***
   *
   * @param infraEntity
   * @param mapper
   * @return Optional.of(infraNode) if runtime inputs are present, else Optional.empty() otherwise
   */
  private Optional<ObjectNode> createInfraDefinitionNodeWithInputs(
      InfrastructureEntity infraEntity, ObjectMapper mapper) {
    String yaml = infraEntity.getYaml();
    if (isEmpty(yaml)) {
      throw new InvalidRequestException(
          "Infrastructure Yaml cannot be empty for infra : " + infraEntity.getIdentifier());
    }
    ObjectNode infraNode = mapper.createObjectNode();
    try {
      String infraDefinitionInputs = RuntimeInputFormHelper.createRuntimeInputForm(yaml, true);
      if (isEmpty(infraDefinitionInputs)) {
        return Optional.empty();
      }

      infraNode.put(IDENTIFIER, infraEntity.getIdentifier());
      YamlField infrastructureDefinitionYamlField =
          YamlUtils.readTree(infraDefinitionInputs).getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
      ObjectNode infraDefinitionNode = (ObjectNode) infrastructureDefinitionYamlField.getNode().getCurrJsonNode();
      infraNode.set(YamlTypes.INPUTS, infraDefinitionNode);
    } catch (IOException e) {
      throw new InvalidRequestException(
          format("Error occurred while creating inputs for infra definition : %s", infraEntity.getIdentifier()), e);
    }
    return Optional.of(infraNode);
  }

  String getDuplicateInfrastructureExistsErrorMessage(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String infraIdentifier) {
    if (EmptyPredicate.isEmpty(orgIdentifier)) {
      return String.format(DUP_KEY_EXP_FORMAT_STRING_FOR_ACCOUNT, infraIdentifier, accountIdentifier);
    } else if (EmptyPredicate.isEmpty(projectIdentifier)) {
      return String.format(DUP_KEY_EXP_FORMAT_STRING_FOR_ORG, infraIdentifier, orgIdentifier, accountIdentifier);
    }
    return String.format(DUP_KEY_EXP_FORMAT_STRING_FOR_PROJECT, infraIdentifier, envIdentifier, projectIdentifier,
        orgIdentifier, accountIdentifier);
  }

  @VisibleForTesting
  String getDuplicateInfrastructureExistsErrorMessage(String accountId, String exceptionString) {
    String errorMessageToBeReturned;
    try {
      JSONObject jsonObjectOfDuplicateKey = DuplicateKeyExceptionParser.getDuplicateKey(exceptionString);
      if (jsonObjectOfDuplicateKey != null) {
        String orgIdentifier = jsonObjectOfDuplicateKey.getString("orgIdentifier");
        String projectIdentifier = jsonObjectOfDuplicateKey.getString("projectIdentifier");
        String envIdentifier = jsonObjectOfDuplicateKey.getString("envIdentifier");
        String identifier = jsonObjectOfDuplicateKey.getString("identifier");
        errorMessageToBeReturned = getDuplicateInfrastructureExistsErrorMessage(
            accountId, orgIdentifier, projectIdentifier, envIdentifier, identifier);
      } else {
        errorMessageToBeReturned = "A Duplicate Infrastructure already exists";
      }
    } catch (Exception ex) {
      errorMessageToBeReturned = "A Duplicate Infrastructure already exists";
    }
    return errorMessageToBeReturned;
  }

  private void validateInfraList(List<InfrastructureEntity> infraEntities) {
    if (isEmpty(infraEntities)) {
      return;
    }
    infraEntities.forEach(
        infraEntity -> validatePresenceOfRequiredFields(infraEntity.getAccountId(), infraEntity.getIdentifier()));
  }

  private void populateDefaultNameIfNotPresent(List<InfrastructureEntity> infraEntities) {
    if (isEmpty(infraEntities)) {
      return;
    }
    infraEntities.forEach(this::setNameIfNotPresent);
  }

  private void modifyInfraRequest(InfrastructureEntity requestInfra) {
    requestInfra.setName(requestInfra.getName().trim());
    // convert to scope of the environment
    String[] envRefSplit = StringUtils.split(requestInfra.getEnvIdentifier(), ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit != null && envRefSplit.length == 2) {
      IdentifierRef envIdentifierRef = IdentifierRefHelper.getIdentifierRef(requestInfra.getEnvIdentifier(),
          requestInfra.getAccountId(), requestInfra.getOrgIdentifier(), requestInfra.getProjectIdentifier());
      requestInfra.setOrgIdentifier(envIdentifierRef.getOrgIdentifier());
      requestInfra.setProjectIdentifier(envIdentifierRef.getProjectIdentifier());
      requestInfra.setEnvIdentifier(envIdentifierRef.getIdentifier());
    }

    // handle empty scope identifiers
    requestInfra.setOrgIdentifier(
        EmptyPredicate.isEmpty(requestInfra.getOrgIdentifier()) ? null : requestInfra.getOrgIdentifier());
    requestInfra.setProjectIdentifier(
        EmptyPredicate.isEmpty(requestInfra.getProjectIdentifier()) ? null : requestInfra.getProjectIdentifier());
  }

  private void modifyInfraRequestBatch(List<InfrastructureEntity> infrastructureEntityList) {
    if (isEmpty(infrastructureEntityList)) {
      return;
    }
    infrastructureEntityList.forEach(this::modifyInfraRequest);
  }

  private Criteria getInfrastructureEqualityCriteriaForEnv(
      String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier) {
    String[] envRefSplit = StringUtils.split(envIdentifier, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      return Criteria.where(InfrastructureEntityKeys.accountId)
          .is(accountId)
          .and(InfrastructureEntityKeys.orgIdentifier)
          .is(orgIdentifier)
          .and(InfrastructureEntityKeys.projectIdentifier)
          .is(projectIdentifier)
          .and(InfrastructureEntityKeys.envIdentifier)
          .is(envIdentifier);
    } else {
      // env ref provided
      IdentifierRef envIdentifierRef =
          IdentifierRefHelper.getIdentifierRef(envIdentifier, accountId, orgIdentifier, projectIdentifier);
      return Criteria.where(InfrastructureEntityKeys.accountId)
          .is(envIdentifierRef.getAccountIdentifier())
          .and(InfrastructureEntityKeys.orgIdentifier)
          .is(envIdentifierRef.getOrgIdentifier())
          .and(InfrastructureEntityKeys.projectIdentifier)
          .is(envIdentifierRef.getProjectIdentifier())
          .and(InfrastructureEntityKeys.envIdentifier)
          .is(envIdentifierRef.getIdentifier());
    }
  }

  private Criteria getInfrastructureEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier) {
    return Criteria.where(InfrastructureEntityKeys.accountId)
        .is(accountId)
        .and(InfrastructureEntityKeys.orgIdentifier)
        .is(orgIdentifier)
        .and(InfrastructureEntityKeys.projectIdentifier)
        .is(projectIdentifier);
  }

  @Override
  public List<InfrastructureYamlMetadata> createInfrastructureYamlMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentRef, List<String> infraIds) {
    List<InfrastructureEntity> infrastructureEntities = new ArrayList<>();
    if (!EngineExpressionEvaluator.hasExpressions(environmentRef)) {
      // this gets just metadata and will be deprecated with remote infrastructure support
      infrastructureEntities = getAllInfrastructureMetadataFromIdentifierList(
          accountId, orgIdentifier, projectIdentifier, environmentRef, infraIds);
    }
    List<InfrastructureYamlMetadata> infrastructureYamlMetadataList = new ArrayList<>();
    infrastructureEntities.forEach(infrastructureEntity
        -> infrastructureYamlMetadataList.add(createInfrastructureYamlMetadataInternal(infrastructureEntity)));
    return infrastructureYamlMetadataList;
  }

  @Override
  public GitEntityInfo getGitDetailsForInfrastructure(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String environmentRef, String environmentBranch) {
    GitEntityInfo defaultGitContext = GitEntityInfo.builder().build();

    Optional<Environment> environmentOptional =
        environmentService.getMetadata(accountIdentifier, orgIdentifier, projectIdentifier, environmentRef, false);

    if (environmentOptional.isEmpty()) {
      return defaultGitContext;
    }

    Environment environment = environmentOptional.get();
    // inline environment will always use infra from default repo
    if (!StoreType.REMOTE.equals(environment.getStoreType())) {
      return defaultGitContext;
    }

    // find the working branch of env when dynamically linked
    String branch = gitAwareEntityHelper.getWorkingBranch(environment.getRepo());

    // set branch to environment branch when statically linked, otherwise working branch of env
    // there is no concept of transient branch for infra since we do not allow branch selection for infra
    defaultGitContext.setBranch(isNotEmpty(environmentBranch) ? environmentBranch : branch);
    defaultGitContext.setParentEntityRepoName(environment.getRepo());

    return defaultGitContext;
  }

  @Override
  public List<InfrastructureYamlMetadata> createInfrastructureYamlMetadata(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String environmentRef, String environmentBranch,
      List<String> infraIds, boolean loadFromCache) {
    if (isEmpty(infraIds)) {
      return Collections.EMPTY_LIST;
    }

    List<InfrastructureYamlMetadata> infrastructureYamlMetadataList = new ArrayList<>();
    try {
      infrastructureYamlMetadataList = createInfrastructureYamlMetadataInternalV2(accountIdentifier, orgIdentifier,
          projectIdentifier, environmentRef, environmentBranch, infraIds, loadFromCache);
    } catch (CompletionException ex) {
      // internal method always wraps the CompletionException, so we will have a cause
      log.error(String.format("Error while getting infrastructure inputs: %s", infraIds), ex);
      Throwables.throwIfUnchecked(ex.getCause());
    } catch (Exception ex) {
      log.error(String.format("Unexpected error occurred while getting infrastructure inputs: %s", infraIds), ex);
      throw new InternalServerErrorException(
          String.format(
              "Unexpected error occurred while getting infrastructure inputs: %s: [%s]", infraIds, ex.getMessage()),
          ex);
    }

    return infrastructureYamlMetadataList;
  }

  private List<InfrastructureYamlMetadata> createInfrastructureYamlMetadataInternalV2(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String environmentRef, String environmentBranch,
      List<String> infraIds, boolean loadFromCache) {
    if (EngineExpressionEvaluator.hasExpressions(environmentRef)) {
      return Collections.EMPTY_LIST;
    }

    try (EntityGitDetailsGuard ignore = new EntityGitDetailsGuard(getGitDetailsForInfrastructure(
             accountIdentifier, orgIdentifier, projectIdentifier, environmentRef, environmentBranch))) {
      return getInfrastructuresYamlInBatches(getAllInfrastructureMetadataFromIdentifierList(accountIdentifier,
                                                 orgIdentifier, projectIdentifier, environmentRef, infraIds),
          loadFromCache);
    }
  }

  private void populateInfrastructuresWithYaml(
      List<InfrastructureEntity> infrastructureEntities, boolean loadFromCache) {
    // Sorting List so that git calls are made parallelly at the earliest.
    List<InfrastructureEntity> sortedInfrastructureEntities = sortByStoreType(infrastructureEntities);
    for (int i = 0; i < sortedInfrastructureEntities.size(); i += REMOTE_INFRASTRUCTURES_BATCH_SIZE) {
      List<InfrastructureEntity> batch = getBatch(sortedInfrastructureEntities, i);

      List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

      for (InfrastructureEntity infra : batch) {
        if (StoreType.REMOTE.equals(infra.getStoreType())) {
          CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // this updates the YAML from remote
            infrastructureRepository.getRemoteInfrastructureWithYaml(infra, loadFromCache, false);
          }, executorService);

          batchFutures.add(future);
        }
      }

      // Wait for the batch to complete
      CompletableFuture<Void> allOf = CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));
      allOf.join();
    }
  }

  private List<InfrastructureYamlMetadata> getInfrastructuresYamlInBatches(
      List<InfrastructureEntity> infrastructureEntities, boolean loadFromCache) {
    // Using SynchronousQueue to avoid ConcurrentModification Issues.
    Queue<InfrastructureYamlMetadata> infrastructureYamlMetadataQueue = new ConcurrentLinkedQueue<>();

    // Sorting List so that git calls are made parallelly at the earliest.
    List<InfrastructureEntity> sortedInfrastructureEntities = sortByStoreType(infrastructureEntities);
    for (int i = 0; i < sortedInfrastructureEntities.size(); i += REMOTE_INFRASTRUCTURES_BATCH_SIZE) {
      List<InfrastructureEntity> batch = getBatch(sortedInfrastructureEntities, i);

      List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

      for (InfrastructureEntity infra : batch) {
        if (StoreType.REMOTE.equals(infra.getStoreType())) {
          CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            InfrastructureEntity infrastructureFromRemote =
                infrastructureRepository.getRemoteInfrastructureWithYaml(infra, loadFromCache, false);
            infrastructureYamlMetadataQueue.add(createInfrastructureYamlMetadataInternal(infrastructureFromRemote));
          }, executorService);

          batchFutures.add(future);
        } else {
          infrastructureYamlMetadataQueue.add(createInfrastructureYamlMetadataInternal(infra));
        }
      }

      // Wait for the batch to complete
      CompletableFuture<Void> allOf = CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));
      allOf.join();
    }
    return fetchResponsesInListFromQueue(infrastructureYamlMetadataQueue);
  }

  private List<InfrastructureEntity> sortByStoreType(List<InfrastructureEntity> infrastructureEntities) {
    List<InfrastructureEntity> sortedInfrastructureEntities = new ArrayList<>();
    for (InfrastructureEntity infra : infrastructureEntities) {
      if (StoreType.REMOTE.equals(infra.getStoreType())) {
        sortedInfrastructureEntities.add(infra);
      }
    }

    for (InfrastructureEntity infra : infrastructureEntities) {
      // StoreType can be null.
      if (!StoreType.REMOTE.equals(infra.getStoreType())) {
        sortedInfrastructureEntities.add(infra);
      }
    }
    return sortedInfrastructureEntities;
  }

  private static List<InfrastructureYamlMetadata> fetchResponsesInListFromQueue(
      Queue<InfrastructureYamlMetadata> envInputYamlAndServiceOverridesQueue) {
    List<InfrastructureYamlMetadata> envInputYamlAndServiceOverridesList = new ArrayList<>();
    while (!envInputYamlAndServiceOverridesQueue.isEmpty()) {
      InfrastructureYamlMetadata element = envInputYamlAndServiceOverridesQueue.poll();
      envInputYamlAndServiceOverridesList.add(element);
    }
    return envInputYamlAndServiceOverridesList;
  }

  public static EntityGitDetails getEntityGitDetails(InfrastructureEntity infrastructureEntity) {
    if (infrastructureEntity.getStoreType() == StoreType.REMOTE) {
      EntityGitDetails entityGitDetails = GitAwareContextHelper.getEntityGitDetails(infrastructureEntity);

      // add additional details from scm metadata
      return GitAwareContextHelper.updateEntityGitDetailsFromScmGitMetadata(entityGitDetails);
    }
    return null; // Default if storeType is not remote
  }

  private static List<InfrastructureEntity> getBatch(List<InfrastructureEntity> infrastructureEntities, int i) {
    int endIndex = Math.min(i + REMOTE_INFRASTRUCTURES_BATCH_SIZE, infrastructureEntities.size());
    return infrastructureEntities.subList(i, endIndex);
  }

  private InfrastructureYamlMetadata createInfrastructureYamlMetadataInternal(
      InfrastructureEntity infrastructureEntity) {
    if (isBlank(infrastructureEntity.getYaml())) {
      log.info(
          "Infrastructure with identifier {} is not configured with an Infrastructure definition. Infrastructure Yaml is empty",
          infrastructureEntity.getIdentifier());
      return InfrastructureYamlMetadata.builder()
          .infrastructureIdentifier(infrastructureEntity.getIdentifier())
          .orgIdentifier(infrastructureEntity.getOrgIdentifier())
          .projectIdentifier(infrastructureEntity.getProjectIdentifier())
          .infrastructureYaml("")
          .inputSetTemplateYaml("")
          .build();
    }

    final String infrastructureInputSetYaml = createInfrastructureInputsYamlInternal(infrastructureEntity);
    return InfrastructureYamlMetadata.builder()
        .infrastructureIdentifier(infrastructureEntity.getIdentifier())
        .infrastructureYaml(infrastructureEntity.getYaml())
        .inputSetTemplateYaml(infrastructureInputSetYaml)
        .orgIdentifier(infrastructureEntity.getOrgIdentifier())
        .projectIdentifier(infrastructureEntity.getProjectIdentifier())
        .entityGitDetails(getEntityGitDetails(infrastructureEntity))
        .connectorRef(infrastructureEntity.getConnectorRef())
        .fallbackBranch(infrastructureEntity.getFallBackBranch())
        .storeType(infrastructureEntity.getStoreType())
        .build();
  }

  @Override
  public String createInfrastructureInputsFromYaml(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, String infraIdentifier) {
    Map<String, Object> yamlInputs = createInfrastructureInputsYamlInternal(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, infraIdentifier);

    if (isEmpty(yamlInputs)) {
      return null;
    }
    return YamlPipelineUtils.writeYamlString(yamlInputs);
  }

  @Override
  public InfrastructureInputsMergedResponseDto mergeInfraStructureInputs(String accountId, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String infraIdentifier, String oldInfrastructureInputsYaml) {
    Optional<InfrastructureEntity> infrastructureEntityOptional =
        get(accountId, orgIdentifier, projectIdentifier, envIdentifier, infraIdentifier);
    if (infrastructureEntityOptional.isEmpty()) {
      throw new NotFoundException(
          format("Infrastructure with identifier [%s] in environment [%s] in project [%s], org [%s] not found",
              infraIdentifier, envIdentifier, projectIdentifier, orgIdentifier));
    }

    InfrastructureEntity infrastructureEntity = infrastructureEntityOptional.get();
    String infraYaml = infrastructureEntity.getYaml();
    if (isEmpty(infraYaml)) {
      return InfrastructureInputsMergedResponseDto.builder()
          .mergedInfrastructureInputsYaml("")
          .infrastructureYaml("")
          .build();
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      Optional<ObjectNode> infraDefinitionNodeWithInputs =
          createInfraDefinitionNodeWithInputs(infrastructureEntity, mapper);

      Map<String, Object> yamlInputs = new HashMap<>();
      infraDefinitionNodeWithInputs.ifPresent(
          jsonNodes -> yamlInputs.put(YamlTypes.INPUTS, jsonNodes.get(YamlTypes.INPUTS)));

      String newInfraInputsYaml =
          isNotEmpty(yamlInputs) ? YamlPipelineUtils.writeYamlString(yamlInputs) : StringUtils.EMPTY;
      boolean allowDifferentInfraForEnvPropagation = ngFeatureFlagHelperService.isEnabled(
          accountId, FeatureName.CDS_SUPPORT_DIFFERENT_INFRA_DURING_ENV_PROPAGATION);
      return InfrastructureInputsMergedResponseDto.builder()
          .mergedInfrastructureInputsYaml(InputSetMergeUtility.mergeArrayNodeInputs(
              oldInfrastructureInputsYaml, newInfraInputsYaml, allowDifferentInfraForEnvPropagation))
          .infrastructureYaml(infraYaml)
          .build();
    } catch (Exception ex) {
      throw new InvalidRequestException("Error occurred while merging old and new infrastructure inputs", ex);
    }
  }

  @Override
  public Page<InfrastructureEntity> getScopedInfrastructures(
      Page<InfrastructureEntity> infrastructureEntities, List<String> serviceRefs) {
    if (CollectionUtils.isEmpty(serviceRefs) || infrastructureEntities.getTotalElements() == 0) {
      return infrastructureEntities;
    }
    // filter out invalid infrastructures from the list
    List<InfrastructureEntity> scopedInfrastructureEntities =
        infrastructureEntities.get()
            .filter(infrastructureEntity -> validateInfrastructureScopes(infrastructureEntity, serviceRefs))
            .collect(Collectors.toList());
    return new PageImpl<>(
        scopedInfrastructureEntities, infrastructureEntities.getPageable(), scopedInfrastructureEntities.size());
  }

  @Override
  public List<String> filterServicesByScopedInfrastructures(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<String> serviceRefs, Map<String, List<String>> envRefInfraRefsMapping) {
    if (MapUtils.isEmpty(envRefInfraRefsMapping) || CollectionUtils.isEmpty(serviceRefs)) {
      return serviceRefs;
    }

    // looping over to each env and filtering out invalid services from the list by validating scopes in infras
    envRefInfraRefsMapping.forEach((key, value)
                                       -> filterServicesForScopedInfra(accountIdentifier, orgIdentifier,
                                           projectIdentifier, serviceRefs, key, value));
    return serviceRefs;
  }

  @Override
  public void checkIfInfraIsScopedToService(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String serviceRef, String envRef, String infraRef) {
    Optional<InfrastructureEntity> infrastructureEntityOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, envRef, infraRef);
    if (infrastructureEntityOptional.isEmpty()) {
      return;
    }
    List<String> scopedToServices = getScopedToServicesField(infrastructureEntityOptional.get());
    if (CollectionUtils.isEmpty(scopedToServices) || scopedToServices.contains(serviceRef)) {
      return;
    }
    throw new InvalidRequestException(
        String.format("Infrastructure: [%s] inside %s level Environment: [%s] can't be scoped"
                + " to %s level Service: [%s]",
            infraRef, IdentifierRefHelper.getScopeFromScopedRef(envRef), envRef,
            IdentifierRefHelper.getScopeFromScopedRef(serviceRef), serviceRef));
  }

  @Override
  public void checkIfInfraIsScopedToService(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<String> serviceRefs, Map<String, List<String>> envRefInfraRefsMapping) {
    if (MapUtils.isEmpty(envRefInfraRefsMapping) || CollectionUtils.isEmpty(serviceRefs)) {
      return;
    }
    // looping over to each env and validating if infras are scoped to given services
    envRefInfraRefsMapping.forEach((key, value)
                                       -> checkIfInfraIsScopedToService(accountIdentifier, orgIdentifier,
                                           projectIdentifier, serviceRefs, key, value));
  }

  private void checkIfInfraIsScopedToService(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<String> serviceRefs, String envRef, List<String> infraRefs) {
    // todo: support for remote infrastructures
    List<InfrastructureEntity> infrastructureEntities = getAllInfrastructureMetadataFromIdentifierList(
        accountIdentifier, orgIdentifier, projectIdentifier, envRef, infraRefs);
    if (CollectionUtils.isEmpty(infrastructureEntities)) {
      return;
    }
    for (InfrastructureEntity infrastructureEntity : infrastructureEntities) {
      List<String> scopedToServices = getScopedToServicesField(infrastructureEntity);
      // if there is no scoping, then all services are valid
      if (CollectionUtils.isEmpty(scopedToServices)) {
        continue;
      }
      List<String> unScopedServices = serviceRefs.stream()
                                          .filter(serviceRef -> !scopedToServices.contains(serviceRef))
                                          .collect(Collectors.toList());

      if (!CollectionUtils.isEmpty(unScopedServices)) {
        throw new InvalidRequestException(
            String.format("Infrastructure: [%s] inside %s level Environment: [%s] can't be scoped"
                    + " to Service: [%s]",
                infrastructureEntity.getIdentifier(), IdentifierRefHelper.getScopeFromScopedRef(envRef), envRef,
                unScopedServices));
      }
    }
  }

  private void filterServicesForScopedInfra(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<String> serviceRefs, String envRef, List<String> infraRefs) {
    // todo: support for remote infrastructures
    List<InfrastructureEntity> infrastructureEntities = getAllInfrastructureMetadataFromIdentifierList(
        accountIdentifier, orgIdentifier, projectIdentifier, envRef, infraRefs);
    if (CollectionUtils.isEmpty(infrastructureEntities)) {
      return;
    }
    for (InfrastructureEntity infrastructureEntity : infrastructureEntities) {
      List<String> scopedToServices = getScopedToServicesField(infrastructureEntity);
      // if there is no scoping, then all services are valid
      if (CollectionUtils.isEmpty(scopedToServices)) {
        continue;
      }
      // if there is scoping, filtering out invalid services
      serviceRefs = serviceRefs.stream().filter(scopedToServices::contains).collect(Collectors.toList());
    }
  }

  private boolean validateInfrastructureScopes(
      InfrastructureEntity infrastructureEntity, @NotNull List<String> serviceRefs) {
    List<String> scopedToServices = getScopedToServicesField(infrastructureEntity);
    // here infra is valid if it is scoped to atleast all given services or if it is not scoped at all.
    return CollectionUtils.isEmpty(scopedToServices) || scopedToServices.containsAll(serviceRefs);
  }

  private List<String> getScopedToServicesField(InfrastructureEntity infrastructureEntity) {
    InfrastructureConfig infrastructureConfig =
        InfrastructureEntityConfigMapper.toInfrastructureConfig(infrastructureEntity);
    return infrastructureConfig.getInfrastructureDefinitionConfig().getScopedServices();
  }

  InfrastructureEntity getInfrastructureFromEnvAndInfraIdentifier(
      String accountId, String orgId, String projectId, String envRef, String infraId) {
    Optional<InfrastructureEntity> infrastructureEntity;
    String[] envRefSplit = StringUtils.split(envRef, ".", MAX_RESULT_THRESHOLD_FOR_SPLIT);
    if (envRefSplit == null || envRefSplit.length == 1) {
      infrastructureEntity =
          infrastructureRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnvIdentifierAndIdentifier(
              accountId, orgId, projectId, envRef, infraId, false, false, false);
    } else {
      IdentifierRef envIdentifierRef = IdentifierRefHelper.getIdentifierRef(envRef, accountId, orgId, projectId);
      infrastructureEntity =
          infrastructureRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnvIdentifierAndIdentifier(
              envIdentifierRef.getAccountIdentifier(), envIdentifierRef.getOrgIdentifier(),
              envIdentifierRef.getProjectIdentifier(), envIdentifierRef.getIdentifier(), infraId, false, false, false);
    }

    return infrastructureEntity.orElse(null);
  }

  private Map<String, Object> createInfrastructureInputsYamlInternal(
      String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier, String infraIdentifier) {
    Map<String, Object> yamlInputs = new HashMap<>();
    InfrastructureEntity infrastructureEntity = getInfrastructureFromEnvAndInfraIdentifier(
        accountId, orgIdentifier, projectIdentifier, envIdentifier, infraIdentifier);
    ObjectNode infraDefinition = createInfraDefinitionNodeWithInputs(infrastructureEntity);
    if (infraDefinition != null) {
      yamlInputs.put("infrastructureInputs", infraDefinition);
    }
    return yamlInputs;
  }

  private String createInfrastructureInputsYamlInternal(InfrastructureEntity infrastructureEntity) {
    Map<String, Object> yamlInputs = new HashMap<>();
    ObjectNode infraDefinition = createInfraDefinitionNodeWithInputs(infrastructureEntity);
    if (infraDefinition != null) {
      yamlInputs.put("infrastructureInputs", infraDefinition);
    }
    return YamlPipelineUtils.writeYamlString(yamlInputs);
  }

  private ObjectNode createInfraDefinitionNodeWithInputs(InfrastructureEntity infraEntity) {
    String yaml = infraEntity.getYaml();
    if (isEmpty(yaml)) {
      throw new InvalidRequestException("Infrastructure Yaml cannot be empty");
    }
    try {
      String infraDefinitionInputs = RuntimeInputFormHelper.createRuntimeInputFormWithDefaultValues(yaml);
      if (isEmpty(infraDefinitionInputs)) {
        return null;
      }

      YamlField infrastructureDefinitionYamlField =
          YamlUtils.readTree(infraDefinitionInputs).getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
      return (ObjectNode) infrastructureDefinitionYamlField.getNode().getCurrJsonNode();
    } catch (IOException e) {
      throw new InvalidRequestException("Error occurred while creating Infrastructure inputs ", e);
    }
  }

  private void processDownstreamDeletions(
      String accountId, String orgIdentifier, String projectIdentifier, String envRef, String infraIdentifier) {
    processQuietly(()
                       -> serviceOverridesServiceV2.deleteAllForInfra(
                           accountId, orgIdentifier, projectIdentifier, envRef, infraIdentifier));
  }

  boolean processQuietly(BooleanSupplier b) {
    try {
      return b.getAsBoolean();
    } catch (Exception ex) {
      // ignore this
    }
    return false;
  }

  @Override
  public InfraMoveConfigResponse moveInfrastructure(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String environmentIdentifier, String infraIdentifier,
      InfraMoveConfigOperationDTO moveConfigOperationDTO) {
    validateMoveConfigRequest(moveConfigOperationDTO);
    setupGitContext(moveConfigOperationDTO);
    applyGitXSettingsIfApplicable(accountIdentifier, orgIdentifier, projectIdentifier);
    if (!cdGitXService.isNewGitXEnabled(accountIdentifier, orgIdentifier, projectIdentifier)) {
      throw new InvalidRequestException(
          GitXUtils.getErrorMessageForGitSimplificationNotEnabled(orgIdentifier, projectIdentifier));
    }

    Optional<InfrastructureEntity> optionalInfrastructure =
        getMetadata(accountIdentifier, orgIdentifier, projectIdentifier, environmentIdentifier, infraIdentifier);

    if (optionalInfrastructure.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Infra with the given identifier: %s and environment: %s does not exist", infraIdentifier,
              environmentIdentifier));
    }

    InfrastructureEntity infrastructure = optionalInfrastructure.get();
    if (StoreType.REMOTE.equals(infrastructure.getStoreType())
        && INLINE_TO_REMOTE.equals(moveConfigOperationDTO.getMoveConfigOperationType())) {
      throw new InvalidRequestException(
          String.format("Infrastructure with the given identifier: %s is already remote", infraIdentifier));
    }

    InfrastructureEntity movedEntity =
        infrastructureRepository.moveInfrastructure(moveConfigOperationDTO, infrastructure);
    return InfraMoveConfigResponse.builder().identifier(movedEntity.getIdentifier()).success(true).build();
  }

  private void setupGitContext(InfraMoveConfigOperationDTO moveConfigDTO) {
    if (INLINE_TO_REMOTE.equals(moveConfigDTO.getMoveConfigOperationType())) {
      GitAwareContextHelper.populateGitDetails(
          GitEntityInfo.builder()
              .branch(moveConfigDTO.getBranch())
              .filePath(moveConfigDTO.getFilePath())
              .commitMsg(moveConfigDTO.getCommitMessage())
              .isNewBranch(isNotEmpty(moveConfigDTO.getBranch()) && isNotEmpty(moveConfigDTO.getBaseBranch()))
              .baseBranch(moveConfigDTO.getBaseBranch())
              .connectorRef(moveConfigDTO.getConnectorRef())
              .storeType(StoreType.REMOTE)
              .repoName(moveConfigDTO.getRepoName())
              .build());
    }
  }

  private void validateMoveConfigRequest(InfraMoveConfigOperationDTO moveConfigOperationDTO) {
    if (INLINE_TO_REMOTE.equals(moveConfigOperationDTO.getMoveConfigOperationType())) {
      if (isEmpty(moveConfigOperationDTO.getFilePath())) {
        ExceptionCreationUtils.throwInvalidRequestForEmptyField("filePath");
      }
    } else {
      throw new UnsupportedOperationException(String.format(
          "Move operation: [%s] not supported for infra", moveConfigOperationDTO.getMoveConfigOperationType()));
    }
  }

  private void applyGitXSettingsIfApplicable(String accountIdentifier, String orgIdentifier, String projIdentifier) {
    gitXSettingsHelper.enforceGitExperienceIfApplicable(accountIdentifier, orgIdentifier, projIdentifier);
    gitXSettingsHelper.setDefaultStoreTypeForEntities(
        accountIdentifier, orgIdentifier, projIdentifier, EntityType.INFRASTRUCTURE);
    gitXSettingsHelper.setConnectorRefForRemoteEntity(accountIdentifier, orgIdentifier, projIdentifier);
    gitXSettingsHelper.setDefaultRepoForRemoteEntity(accountIdentifier, orgIdentifier, projIdentifier);
  }
}
