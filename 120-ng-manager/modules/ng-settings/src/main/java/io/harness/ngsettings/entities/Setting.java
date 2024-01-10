/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.entities;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.entities.UserSetting.UserSettingKeys;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "SettingKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "settings", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("settings")
@Persistent
public abstract class Setting implements PersistentEntity, NGAccountAccess {
  @Id @dev.morphia.annotations.Id String id;
  @NotEmpty @EntityIdentifier String identifier;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  @NotNull SettingCategory category;
  String groupIdentifier;
  @LastModifiedDate Long lastModifiedAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountIdentifier_orgIdentifier_projectIdentifier_identifier_userId_unique_idx")
                 .field(SettingKeys.accountIdentifier)
                 .field(SettingKeys.orgIdentifier)
                 .field(SettingKeys.projectIdentifier)
                 .field(SettingKeys.identifier)
                 .field(UserSettingKeys.userId)
                 .unique(true)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("accountIdentifier_orgIdentifier_projectIdentifier_category_idx")
                 .field(SettingKeys.accountIdentifier)
                 .field(SettingKeys.orgIdentifier)
                 .field(SettingKeys.projectIdentifier)
                 .field(SettingKeys.category)
                 .build())
        .build();
  }

  @Override
  public String getAccountIdentifier() {
    return this.accountIdentifier;
  }
}