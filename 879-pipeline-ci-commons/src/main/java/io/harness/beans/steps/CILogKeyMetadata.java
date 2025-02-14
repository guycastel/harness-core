/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CI)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "CILogKeyMetadataKeys")
@StoreIn(DbAliases.CIMANAGER)
@Entity(value = "ciLogKeyMetadata", noClassnameStored = true)
@Document("ciLogKeyMetadata")
@TypeAlias("ciLogKeyMetadata")
@HarnessEntity(exportable = true)
public class CILogKeyMetadata implements PersistentEntity, UuidAware {
  @Id @org.springframework.data.annotation.Id String uuid;

  @FdIndex String stageExecutionId;
  List<String> logKeys;

  @Builder.Default
  @FdTtlIndex
  private Date expireAfter = Date.from(OffsetDateTime.now().plusSeconds(86400).toInstant());
}
