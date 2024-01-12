/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoConfig.NO_LIMIT;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.data.structure.UUIDGenerator;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.entities.migration.NGManagerUniqueIdParentIdMigrationStatus;
import io.harness.ng.core.entities.migration.NGManagerUniqueIdParentIdMigrationStatus.NGManagerUniqueIdParentIdMigrationStatusKeys;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.persistence.UniqueIdAccess;
import io.harness.persistence.UniqueIdAware;

import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AddUniqueIdParentIdToEntitiesTask implements Runnable {
  private final MongoTemplate mongoTemplate;
  private final PersistentLocker persistentLocker;
  private static final String PARENT_UNIQUE_ID_KEY = "parentUniqueId";
  private static final String LOCK_NAME_PREFIX = "NGEntitiesPeriodicMigrationTaskLock";
  private static final String NG_MANAGER_ENTITIES_MIGRATION_LOG =
      "[NGManagerAddUniqueIdAndParentUniqueIdToEntitiesTask]:";
  private static final int BATCH_SIZE = 500;

  private static final Map<Class<? extends UniqueIdAware>, List<String>> entityWithOrgProjectKeysMap =
      Map.of(Organization.class, new ArrayList<>(), Project.class, List.of(NGCommonEntityConstants.ORG_KEY),
          Connector.class, List.of(NGCommonEntityConstants.ORG_KEY, NGCommonEntityConstants.PROJECT_KEY),
          ServiceAccount.class, List.of(NGCommonEntityConstants.ORG_KEY, NGCommonEntityConstants.PROJECT_KEY));

  @Inject
  public AddUniqueIdParentIdToEntitiesTask(MongoTemplate mongoTemplate, PersistentLocker persistentLocker) {
    this.mongoTemplate = mongoTemplate;
    this.persistentLocker = persistentLocker;
  }

  @Override
  public void run() {
    log.info(format("%s starting...", NG_MANAGER_ENTITIES_MIGRATION_LOG));

    for (Map.Entry<Class<? extends UniqueIdAware>, List<String>> entityMapEntry :
        entityWithOrgProjectKeysMap.entrySet()) {
      String orgIdentifierFieldName = null;
      String projectIdentifierFieldName = null;
      List<String> orgProjectKeysValue = entityMapEntry.getValue();
      if (isNotEmpty(orgProjectKeysValue)) {
        if (orgProjectKeysValue.size() == 2) {
          orgIdentifierFieldName = orgProjectKeysValue.get(0);
          projectIdentifierFieldName = orgProjectKeysValue.get(1);
        } else if (orgProjectKeysValue.size() == 1) {
          orgIdentifierFieldName = orgProjectKeysValue.get(0);
        }
      }

      Class<? extends UniqueIdAware> clazz = entityMapEntry.getKey();
      final String typeAliasName = getTypeAliasValueOrNameForClass(clazz);
      NGManagerUniqueIdParentIdMigrationStatus foundEntity = mongoTemplate.findOne(
          new Query(Criteria.where(NGManagerUniqueIdParentIdMigrationStatusKeys.entityClassName).is(typeAliasName)),
          NGManagerUniqueIdParentIdMigrationStatus.class);
      if (foundEntity == null) {
        foundEntity = NGManagerUniqueIdParentIdMigrationStatus.builder()
                          .entityClassName(typeAliasName)
                          .parentIdMigrationCompleted(Boolean.FALSE)
                          .uniqueIdMigrationCompleted(Boolean.FALSE)
                          .build();
      }

      if (foundEntity.getUniqueIdMigrationCompleted()) {
        log.info(format("%s job for uniqueId on Entity Type: [%s] already completed.",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()));
      } else {
        performUniqueIdMigrationTask(foundEntity, clazz);
      }

      if (foundEntity.getParentIdMigrationCompleted()) {
        log.info(format("%s job for parentId on Entity Type: [%s] already completed.",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()));
      } else {
        performParentIdMigrationTask(foundEntity, clazz, orgIdentifierFieldName, projectIdentifierFieldName);
      }
    }
  }

  private void performUniqueIdMigrationTask(
      NGManagerUniqueIdParentIdMigrationStatus migrationStatusEntity, final Class<? extends UniqueIdAware> clazz) {
    log.info(format(
        "%s Starting uniqueId migration for Entity: [%s]", NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()));

    int migratedCounter = 0;
    int batchSizeCounter = 0;
    int toUpdateCounter = 0;
    int skippedCounter = 0;

    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(LOCK_NAME_PREFIX, Duration.ofSeconds(5))) {
      if (lock == null) {
        log.info(format("%s failed to acquire lock for Entity type: [%s] during uniqueId migration task",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()));
        return;
      }
      try {
        Query documentQuery = new Query(new Criteria());
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz);
        String idValue = null;
        try (CloseableIterator<? extends UniqueIdAware> iterator =
                 mongoTemplate.stream(documentQuery.limit(MongoConfig.NO_LIMIT).maxTimeMsec(MAX_VALUE), clazz)) {
          while (iterator.hasNext()) {
            try {
              UniqueIdAware entity = iterator.next();
              if (isEmpty(entity.getUniqueId())) {
                idValue = getValueOfFieldInEntity(clazz, NGCommonEntityConstants.ENTITY_ID_FIELD_NAME, entity);
                if (isEmpty(idValue)) {
                  // multiple entities have 'uuid' field instead of 'id' field
                  idValue = getValueOfFieldInEntity(clazz, NGCommonEntityConstants.UUID, entity);
                }
                if (isNotEmpty(idValue)) {
                  toUpdateCounter++;
                  batchSizeCounter++;
                  Update update = new Update().set(UniqueIdAccess.UNIQUE_ID_KEY, UUIDGenerator.generateUuid());
                  bulkOperations.updateOne(new Query(Criteria.where("_id").is(idValue)), update);
                  if (batchSizeCounter == BATCH_SIZE) {
                    migratedCounter += bulkOperations.execute().getModifiedCount();
                    bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz);
                    batchSizeCounter = 0;
                  }
                }
              }
            } catch (MappingInstantiationException | IllegalArgumentException exc) {
              log.debug(
                  format(
                      "%s job for uniqueId migration on Entity: [%s], encountered non-supported typeAlias or wrong arguments, skipping entity document",
                      NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()),
                  exc);
              skippedCounter++;
            }
          }
          if (batchSizeCounter > 0) { // for the last remaining batch of entities
            migratedCounter += bulkOperations.execute().getModifiedCount();
          }
        } catch (Exception e) {
          log.error(format("%s job for uniqueId failed to iterate over entities of Entity Type [%s]",
                        NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()),
              e);
          return;
        }
      } catch (Exception exc) {
        log.error(format("%s job for uniqueId failed on Entity Type [%s]", NG_MANAGER_ENTITIES_MIGRATION_LOG,
                      clazz.getSimpleName()),
            exc);
        return;
      }
    }

    if (toUpdateCounter == migratedCounter) {
      migrationStatusEntity.setUniqueIdMigrationCompleted(Boolean.TRUE);
      log.info(format(
          "%s job on entity [%s] for uniqueId Succeeded. Documents to Update and Successful: [%d], Skipped: [%d]",
          NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), toUpdateCounter, skippedCounter));
    } else {
      log.warn(format(
          "%s job failed on entity [%s] for uniqueId. Documents to Update: [%d], Successful: [%d], Failed: [%d], Skipped: [%d]",
          NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), toUpdateCounter, migratedCounter,
          toUpdateCounter - migratedCounter, skippedCounter));
    }
    mongoTemplate.save(migrationStatusEntity);
  }

  private void performParentIdMigrationTask(NGManagerUniqueIdParentIdMigrationStatus foundEntity,
      final Class<? extends UniqueIdAware> clazz, final String orgIdentifierFieldName,
      final String projectIdentifierFieldName) {
    if (clazz == Organization.class && foundEntity.getEntityClassName() != null
        && foundEntity.getEntityClassName().equals(Organization.class.getName())) {
      performOrganizationParentUniqueIdMigrationTask(foundEntity);
    } else if (clazz == Project.class && foundEntity.getEntityClassName() != null
        && foundEntity.getEntityClassName().equals(Project.class.getName())) {
      performProjectParentUniqueIdMigrationTask(foundEntity);
    } else {
      performEntityParentUniqueIdMigrationTask(foundEntity, clazz, orgIdentifierFieldName, projectIdentifierFieldName);
    }
  }

  private void performProjectParentUniqueIdMigrationTask(NGManagerUniqueIdParentIdMigrationStatus foundEntity) {
    int migratedCounter = 0;
    int updateCounter = 0;
    int batchSizeCounter = 0;
    int skippedCounter = 0;
    final String LOCAL_MAP_DELIMITER = "|";

    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(LOCK_NAME_PREFIX, Duration.ofSeconds(5))) {
      if (lock == null) {
        log.info(format("%s failed to acquire lock for Entity type: [%s] during parentId migration task",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, "Project"));
        return;
      }
      try {
        final Map<String, String> orgIdentifierUniqueIdMap = new HashMap<>();

        Query documentQuery = new Query(new Criteria());
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Project.class);
        // iterate over all Project documents
        try (CloseableIterator<Project> iterator =
                 mongoTemplate.stream(documentQuery.limit(NO_LIMIT).maxTimeMsec(MAX_VALUE), Project.class)) {
          while (iterator.hasNext()) {
            Project nextProject = iterator.next();
            if (isEmpty(nextProject.getParentUniqueId())) {
              updateCounter++;
              final String mapKey =
                  nextProject.getAccountIdentifier() + LOCAL_MAP_DELIMITER + nextProject.getOrgIdentifier();
              String uniqueIdOfOrg = null;
              // check if Org with uniqueId is present locally
              if (orgIdentifierUniqueIdMap.containsKey(mapKey)) {
                uniqueIdOfOrg = orgIdentifierUniqueIdMap.get(mapKey);
              } else {
                Criteria orgCriteria = Criteria.where("accountIdentifier")
                                           .is(nextProject.getAccountIdentifier())
                                           .and("identifier")
                                           .is(nextProject.getOrgIdentifier());
                Organization organization = mongoTemplate.findOne(new Query(orgCriteria), Organization.class);
                if (organization != null && isNotEmpty(organization.getUniqueId())) {
                  uniqueIdOfOrg = organization.getUniqueId();
                  orgIdentifierUniqueIdMap.put(mapKey, uniqueIdOfOrg);
                } else {
                  log.warn(format(
                      "%s For EntityType: %s and ParentType: %s having identifier: %s, not found or uniqueId on parent not present. Skipping...",
                      NG_MANAGER_ENTITIES_MIGRATION_LOG, "Project", "Organization", nextProject.getOrgIdentifier()));
                  skippedCounter++;
                }
              }

              if (isNotEmpty(uniqueIdOfOrg)) {
                batchSizeCounter++;
                Update update = new Update().set(ProjectKeys.parentUniqueId, uniqueIdOfOrg);
                bulkOperations.updateOne(new Query(Criteria.where("_id").is(nextProject.getId())), update);

                if (batchSizeCounter == BATCH_SIZE) {
                  migratedCounter += bulkOperations.execute().getModifiedCount();
                  bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Project.class);
                  batchSizeCounter = 0;
                }
              }
            }
          }
          if (batchSizeCounter > 0) { // for the last remaining batch of projects
            migratedCounter += bulkOperations.execute().getModifiedCount();
          }
        } catch (Exception exc) {
          log.error(format("%s task failed to iterate over entities of Entity Type: [%s]",
                        NG_MANAGER_ENTITIES_MIGRATION_LOG, "Project"),
              exc);
          return;
        }
      } catch (Exception exc) {
        log.error(format("%s task failed for Entity Type [%s]", NG_MANAGER_ENTITIES_MIGRATION_LOG, "Project"), exc);
        return;
      }
      log.info(format("%s task on entity [%s] for parentId. Successful: [%d], Failed: [%d], Skipped: [%d]",
          NG_MANAGER_ENTITIES_MIGRATION_LOG, "Project", migratedCounter,
          updateCounter - (migratedCounter + skippedCounter), skippedCounter));
      foundEntity.setParentIdMigrationCompleted(Boolean.TRUE);
      mongoTemplate.save(foundEntity);
    }
  }

  private void performEntityParentUniqueIdMigrationTask(NGManagerUniqueIdParentIdMigrationStatus foundEntity,
      final Class<? extends UniqueIdAware> clazz, final String orgIdentifierFieldName,
      final String projectIdentifierFieldName) {
    int migratedCounter = 0;
    int toUpdateCounter = 0;
    int batchSizeCounter = 0;
    int skippedCounter = 0;
    int orphanEntityCounter = 0;
    final String LOCAL_MAP_DELIMITER = "|";

    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(LOCK_NAME_PREFIX, Duration.ofSeconds(5))) {
      if (lock == null) {
        log.info(format("%s failed to acquire lock for Entity type: [%s] during parentId migration task",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()));
        return;
      }
      try {
        final Map<String, String> scopeEntityUniqueIdMap = new HashMap<>();

        Query documentQuery = new Query(new Criteria());
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz);
        // iterate over all entity documents
        try (CloseableIterator<? extends UniqueIdAware> iterator =
                 mongoTemplate.stream(documentQuery.limit(NO_LIMIT).maxTimeMsec(MAX_VALUE), clazz)) {
          while (iterator.hasNext()) {
            try {
              UniqueIdAware nextEntity = iterator.next();
              // for rest of the entities which has 'parentUniqueId' & at least 'accountIdentifier' present
              if (classHasField(clazz, PARENT_UNIQUE_ID_KEY)
                  && classHasField(clazz, NGCommonEntityConstants.ACCOUNT_KEY)) {
                String mapKey = null;
                String account = getValueOfFieldInEntity(clazz, NGCommonEntityConstants.ACCOUNT_KEY, nextEntity);
                String org = getValueOfFieldInEntity(clazz, orgIdentifierFieldName, nextEntity);
                String proj = getValueOfFieldInEntity(clazz, projectIdentifierFieldName, nextEntity);

                String parentUniqueId = getValueOfFieldInEntity(clazz, PARENT_UNIQUE_ID_KEY, nextEntity);
                if (isEmpty(parentUniqueId)) {
                  toUpdateCounter++;
                  if (isNotEmpty(org) && isNotEmpty(proj)) {
                    mapKey = account + LOCAL_MAP_DELIMITER + org + LOCAL_MAP_DELIMITER + proj;
                  } else if (isNotEmpty(org)) {
                    mapKey = account + LOCAL_MAP_DELIMITER + org;
                  } else {
                    mapKey = account;
                  }

                  String scopeUniqueId = null;
                  if (scopeEntityUniqueIdMap.containsKey(mapKey)) {
                    scopeUniqueId = scopeEntityUniqueIdMap.get(mapKey);
                  } else {
                    Criteria entityCriteria = Criteria.where(NGCommonEntityConstants.ACCOUNT_KEY).is(account);
                    if (isNotEmpty(org) && isNotEmpty(proj)) {
                      entityCriteria.and(orgIdentifierFieldName)
                          .is(org)
                          .and(NGCommonEntityConstants.IDENTIFIER_KEY)
                          .is(proj);
                    } else if (isNotEmpty(org)) {
                      entityCriteria.and(NGCommonEntityConstants.IDENTIFIER_KEY).is(org);
                    }

                    if (isNotEmpty(org) && isNotEmpty(proj)) {
                      Project project = mongoTemplate.findOne(new Query(entityCriteria), Project.class);
                      if (null != project && isNotEmpty(project.getUniqueId())) {
                        scopeUniqueId = project.getUniqueId();
                      } else {
                        // orphan entities under PROJECT
                        log.debug(format(
                            "%s For EntityType: [%s], and ParentType: %s with identifier: %s, parent not found or uniqueId on parent not present. Skipping...",
                            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), "Project", proj));
                        orphanEntityCounter++;
                      }
                    } else if (isNotEmpty(org)) {
                      Organization organization = mongoTemplate.findOne(new Query(entityCriteria), Organization.class);
                      if (null != organization && isNotEmpty(organization.getUniqueId())) {
                        scopeUniqueId = organization.getUniqueId();
                      } else {
                        // orphan entities under ORGANIZATION
                        log.debug(format(
                            "%s For EntityType: [%s], and ParentType: %s with identifier: %s, parent not found or uniqueId on parent not present. Skipping...",
                            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), "Organization", org));
                        orphanEntityCounter++;
                      }
                    } else {
                      scopeUniqueId = account;
                    }
                  }

                  String idValue =
                      getValueOfFieldInEntity(clazz, NGCommonEntityConstants.ENTITY_ID_FIELD_NAME, nextEntity);
                  if (isEmpty(idValue)) {
                    // multiple entities have 'uuid' field instead of 'id' field
                    idValue = getValueOfFieldInEntity(clazz, NGCommonEntityConstants.UUID, nextEntity);
                  }
                  if (isNotEmpty(scopeUniqueId) && isNotEmpty(idValue)) {
                    scopeEntityUniqueIdMap.put(mapKey, scopeUniqueId);
                    // non-scope entities update logic
                    batchSizeCounter++;
                    Update update = new Update().set(PARENT_UNIQUE_ID_KEY, scopeUniqueId);
                    bulkOperations.updateOne(new Query(Criteria.where("_id").is(idValue)), update);
                    if (batchSizeCounter == BATCH_SIZE) {
                      migratedCounter += bulkOperations.execute().getModifiedCount();
                      bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz);
                      batchSizeCounter = 0;
                    }
                  }
                }
              }
            } catch (MappingInstantiationException | IllegalArgumentException exc) {
              log.debug(
                  format(
                      "%s job for parentUniqueId migration on Entity: [%s], encountered non-supported typeAlias or wrong arguments, skipping entity document",
                      NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()),
                  exc);
              skippedCounter++;
            }
          }
          if (batchSizeCounter > 0) { // for the last remaining batch of entities
            migratedCounter += bulkOperations.execute().getModifiedCount();
          }
        } catch (Exception exc) {
          log.error(
              format("%s task failed to iterate over entities during parentUniqueId migration of Entity Type: [%s]",
                  NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()),
              exc);
          return;
        }
      } catch (Exception exc) {
        log.error(format("%s task failed during parentUniqueId migration for Entity Type [%s]",
                      NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName()),
            exc);
        return;
      }

      if (toUpdateCounter == migratedCounter + orphanEntityCounter) {
        foundEntity.setParentIdMigrationCompleted(Boolean.TRUE);
        log.info(format(
            "%s job on entity [%s] for parentUniqueId Succeeded. Documents to Update: [%d], Successful: [%d], Orphan: [%d], Skipped: [%d]",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), toUpdateCounter, migratedCounter,
            orphanEntityCounter, skippedCounter));
      } else {
        log.warn(format(
            "%s job failed on entity [%s] for parentUniqueId. Documents to Update: [%d], Successful: [%d], Failed: [%d], Orphan: [%d], Skipped: [%d]",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), toUpdateCounter, migratedCounter,
            toUpdateCounter - (migratedCounter + orphanEntityCounter), orphanEntityCounter, skippedCounter));
      }
      mongoTemplate.save(foundEntity);
    }
  }

  private void performOrganizationParentUniqueIdMigrationTask(NGManagerUniqueIdParentIdMigrationStatus foundEntity) {
    int migratedCounter = 0;
    int updateCounter = 0;
    int batchSizeCounter = 0;

    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(LOCK_NAME_PREFIX, Duration.ofSeconds(5))) {
      if (lock == null) {
        log.info(format("%s failed to acquire lock for Entity type: [%s] during parentId migration task",
            NG_MANAGER_ENTITIES_MIGRATION_LOG, "Organization"));
        return;
      }
      try {
        Query documentQuery = new Query(new Criteria());
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Organization.class);
        String idValue = null;
        try (CloseableIterator<Organization> iterator =
                 mongoTemplate.stream(documentQuery.limit(NO_LIMIT).maxTimeMsec(MAX_VALUE), Organization.class)) {
          while (iterator.hasNext()) {
            Organization nextOrg = iterator.next();
            if (null != nextOrg && isEmpty(nextOrg.getParentUniqueId())) {
              idValue = nextOrg.getId();
              updateCounter++;
              batchSizeCounter++;
              Update update = new Update().set(OrganizationKeys.parentUniqueId, nextOrg.getAccountIdentifier());
              bulkOperations.updateOne(new Query(Criteria.where("_id").is(idValue)), update);
              if (batchSizeCounter == BATCH_SIZE) {
                migratedCounter += bulkOperations.execute().getModifiedCount();
                bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Organization.class);
                batchSizeCounter = 0;
              }
            }
          }
          if (batchSizeCounter > 0) { // for the last remaining batch of entities
            migratedCounter += bulkOperations.execute().getModifiedCount();
          }
        } catch (Exception exc) {
          log.error(
              format("%s job failed for Entity Type [%s]", NG_MANAGER_ENTITIES_MIGRATION_LOG, "Organization"), exc);
          return;
        }
      } catch (Exception exc) {
        log.error(format("%s job failed for Entity Type [%s]", NG_MANAGER_ENTITIES_MIGRATION_LOG, "Organization"), exc);
        return;
      }
    }
    log.info(format("%s task on entity [%s] for parentId. Successful: [%d], Failed: [%d]",
        NG_MANAGER_ENTITIES_MIGRATION_LOG, "Organization", migratedCounter, updateCounter - migratedCounter));
    foundEntity.setParentIdMigrationCompleted(Boolean.TRUE);
    mongoTemplate.save(foundEntity);
  }

  private String getTypeAliasValueOrNameForClass(Class<? extends UniqueIdAware> clazz) {
    if (clazz.isAnnotationPresent(TypeAlias.class)) {
      TypeAlias annotation = clazz.getAnnotation(TypeAlias.class);
      return annotation.value();
    }
    return clazz.getName();
  }

  private boolean classHasField(final Class<? extends UniqueIdAware> clazz, final String fieldName) {
    return Arrays.stream(clazz.getDeclaredFields())
        .map(Field::getName)
        .anyMatch(f -> nonNull(f) && f.equals(fieldName));
  }

  private String getValueOfFieldInEntity(
      Class<? extends UniqueIdAware> clazz, final String fieldName, UniqueIdAware entity) {
    if (!classHasField(clazz, fieldName)) {
      return null;
    }
    String value = null;
    try {
      Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      value = (String) field.get(entity);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      log.warn(format("%s For EntityType: [%s], cannot get or access value for field: [%s]",
          NG_MANAGER_ENTITIES_MIGRATION_LOG, clazz.getSimpleName(), fieldName));
    }
    return value;
  }
}
