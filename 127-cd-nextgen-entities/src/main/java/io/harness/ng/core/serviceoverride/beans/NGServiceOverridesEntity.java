/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.serviceoverride.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.Trimmed;
import io.harness.gitsync.beans.StoreType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.ScopeAware;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverrideSpecConfig;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesSpec;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.gitaware.GitAware;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.With;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@Data
@Builder
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "serviceOverridesNG", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "NGServiceOverridesEntityKeys")
@Document("serviceOverridesNG")
@TypeAlias("io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity")
@RecasterAlias("io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity")
@OwnedBy(CDC)
public class NGServiceOverridesEntity implements PersistentEntity, ScopeAware, GitAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationIdentifier_projectIdentifier_identifier")
                 .unique(true)
                 .field(NGServiceOverridesEntityKeys.accountId)
                 .field(NGServiceOverridesEntityKeys.orgIdentifier)
                 .field(NGServiceOverridesEntityKeys.projectIdentifier)
                 .field(NGServiceOverridesEntityKeys.identifier)
                 .build())
        .build();
  }

  @Wither @Id @dev.morphia.annotations.Id String id;
  @Trimmed @NotEmpty String accountId;

  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  @Trimmed String serviceRef;
  @NotNull @Trimmed String environmentRef;

  String yaml;

  // for Override 2.0
  @EntityIdentifier String identifier;
  @Trimmed String infraIdentifier;
  @Trimmed String clusterIdentifier; // will infer scope of the cluster from environment
  ServiceOverridesSpec spec;
  ServiceOverridesType type;
  /*
  yamlInternal - This field is only used for create and update API calls via terraform provider (as well as to handle
  override v1 automation). We take overrides spec yaml from user and then convert it to the spec object for further
  processing.
   */
  String yamlInternal;

  /*
  yamlV2 - new field introduced to onboard yaml version of override v2
   */
  String yamlV2;

  @Builder.Default Boolean isV2 = Boolean.FALSE;

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;

  // GitX field
  @With @Setter @NonFinal StoreType storeType;
  @With @Setter @NonFinal String repo;
  @With @Setter @NonFinal String connectorRef;
  @With @Setter @NonFinal String repoURL;
  @With @Setter @NonFinal String fallBackBranch;
  @Setter @NonFinal String filePath;

  @Override
  public String getData() {
    return yamlV2;
  }

  @Override
  public void setData(String data) {
    this.yamlV2 = data;
    setSpecFromYamlV2();
  }

  private void setSpecFromYamlV2() {
    try {
      ServiceOverrideSpecConfig specConfig = YamlUtils.read(this.yamlV2, ServiceOverrideSpecConfig.class);
      // overwrite spec from yamlV2
      this.spec = specConfig.getSpec();
    } catch (IOException ex) {
      log.error(
          String.format("Could not set spec from YAML for override : [%s] of type [%s]", this.identifier, this.type),
          ex);
    }
  }
}
