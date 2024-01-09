/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ssca.entities.artifact.ArtifactEntity;
import io.harness.ssca.entities.artifact.ArtifactEntity.ArtifactEntityKeys;
import io.harness.ssca.services.ArtifactService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@Slf4j
@OwnedBy(HarnessTeam.SSCA)
public class ArtifactTypeMigration implements NGMigration {
  @Inject MongoTemplate mongoTemplate;
  @Inject ArtifactService artifactService;
  private static final String DEBUG_LOG = "TYPE_FIELD_MIGRATION: ";
  @Override
  public void migrate() {
    log.info(DEBUG_LOG + "Starting migration to update empty tag field in ssca artifacts");
    Criteria criteria = Criteria.where(ArtifactEntityKeys.type).ne("image");

    CloseableIterator<ArtifactEntity> iterator = mongoTemplate.stream(new Query(criteria), ArtifactEntity.class);

    while (iterator.hasNext()) {
      ArtifactEntity artifact = iterator.next();
      try {
        artifact.setType("image");
        artifactService.saveArtifact(artifact);
      } catch (Exception e) {
        log.error(String.format("%s Error while migrating artifact _id: %s", DEBUG_LOG, artifact.getId()), e);
      }
    }
    log.info(DEBUG_LOG + "Migration to update type field in ssca artifacts completed");
  }
}
