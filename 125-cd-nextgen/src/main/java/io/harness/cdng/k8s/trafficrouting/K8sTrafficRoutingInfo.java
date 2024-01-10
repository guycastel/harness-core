/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.k8s.trafficrouting;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.k8s.trafficrouting.TrafficRoutingInfoDTO;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "K8sTrafficRoutingInfoKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "k8sTrafficRoutingInfo", noClassnameStored = true)
@Document("k8sTrafficRoutingInfo")
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false,
    components = {HarnessModuleComponent.CDS_PIPELINE, HarnessModuleComponent.CDS_K8S})
public class K8sTrafficRoutingInfo implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id String uuid;
  @NotNull long createdAt;
  @NotNull String accountId;
  @NotNull String orgId;
  @NotNull String projectId;
  @NotNull String stageExecutionId;
  String releaseName;
  TrafficRoutingInfoDTO trafficRoutingInfoDTO;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(3).toInstant());

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_stageExecutionId_createdAt")
                 .field(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.accountId)
                 .field(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.orgId)
                 .field(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.projectId)
                 .field(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.stageExecutionId)
                 .descSortField(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.createdAt)
                 .build())
        .build();
  }
}
