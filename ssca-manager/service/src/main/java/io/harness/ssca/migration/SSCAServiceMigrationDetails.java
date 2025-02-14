/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
@OwnedBy(HarnessTeam.CDC)
public class SSCAServiceMigrationDetails implements MigrationDetails {
  @Override
  public MigrationType getMigrationTypeName() {
    return MigrationType.MongoBGMigration;
  }

  @Override
  public boolean isBackground() {
    return true;
  }

  @Override
  public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
        .add(Pair.of(1, NoopSSCAServiceMigration.class))
        .add(Pair.of(2, ArtifactEntityMigration.class))
        .add(Pair.of(3, EnforcementSummaryCreatedTimeMigration.class))
        .add(Pair.of(4, EnforcementSummaryProjectParamMigration.class))
        .add(Pair.of(5, ArtifactEnvCountMigration.class))
        .add(Pair.of(6, ArtifactNullEnvCountMigration.class))
        .add(Pair.of(7, AddInvalidFieldToArtifactEntity.class))
        .add(Pair.of(8, AddEnvCountsToArtifactEntity.class))
        .add(Pair.of(9, ArtifactEmptyTagMigration.class))
        .add(Pair.of(10, RemoveDuplicateOrchestrationIdFromArtifact.class))
        .add(Pair.of(11, RemoveDuplicateIdFromEnforcementSummary.class))
        .add(Pair.of(12, ArtifactTypeMigration.class))
        .build();
  }
}
