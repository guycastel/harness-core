/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.entities;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.entities.releasedetailsinfo.ReleaseDetails;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ReleaseDetailsMappingNGKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "releaseDetailsMappingNG", noClassnameStored = true)
@Document("releaseDetailsMappingNG")
@Persistent
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class ReleaseDetailsMapping {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_orgIdentifier_projectIdentifier_releaseKey_infraKey_unique_idx")
                 .unique(true)
                 .field(ReleaseDetailsMappingNGKeys.accountIdentifier)
                 .field(ReleaseDetailsMappingNGKeys.orgIdentifier)
                 .field(ReleaseDetailsMappingNGKeys.projectIdentifier)
                 .field(ReleaseDetailsMappingNGKeys.releaseKey)
                 .field(ReleaseDetailsMappingNGKeys.infraKey)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("accountId_releaseKey_idx")
                 .field(ReleaseDetailsMappingNGKeys.accountIdentifier)
                 .field(ReleaseDetailsMappingNGKeys.releaseKey)
                 .build())
        .build();
  }

  @Id @dev.morphia.annotations.Id private String id;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String releaseKey;
  private String infraKey;
  private ReleaseDetails releaseDetails;
}
