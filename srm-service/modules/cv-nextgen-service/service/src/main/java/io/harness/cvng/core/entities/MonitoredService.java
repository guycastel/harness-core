/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.core.beans.template.TemplateMetadata;
import io.harness.cvng.core.beans.template.TemplateMetadata.TemplateMetadataKeys;
import io.harness.cvng.notification.beans.NotificationRuleRef;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "MonitoredServiceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@StoreIn(DbAliases.CVNG)
@Entity(value = "monitoredServices", noClassnameStored = true)
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.CV)
public final class MonitoredService implements PersistentEntity, UuidAware, AccountAccess, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("identifier_idx")
                 .field(MonitoredServiceKeys.accountId)
                 .field(MonitoredServiceKeys.orgIdentifier)
                 .field(MonitoredServiceKeys.projectIdentifier)
                 .field(MonitoredServiceKeys.identifier)
                 .unique(true)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("reconciliation_idx")
                 .field(MonitoredServiceKeys.accountId)
                 .field(MonitoredServiceKeys.orgIdentifier)
                 .field(MonitoredServiceKeys.projectIdentifier)
                 .field(MonitoredServiceKeys.templateMetadata + "." + TemplateMetadataKeys.isTemplateByReference)
                 .field(MonitoredServiceKeys.templateMetadata + "." + TemplateMetadataKeys.templateIdentifier)
                 .field(MonitoredServiceKeys.templateMetadata + "." + TemplateMetadataKeys.versionLabel)
                 .build())
        .build();
  }
  @Id private String uuid;
  String identifier;
  String name;
  String desc;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String serviceIdentifier;
  List<String> environmentIdentifierList;
  MonitoredServiceType type;
  List<String> healthSourceIdentifiers;
  List<String> changeSourceIdentifiers;
  private long lastUpdatedAt;
  private long createdAt;
  private boolean enabled;
  private long lastDisabledAt;
  List<NotificationRuleRef> notificationRuleRefs;
  @FdIndex private long nextNotificationIteration;
  @FdIndex private long nextErrorTrackingNotificationIteration;
  String templateIdentifier;
  String templateVersionLabel;
  TemplateMetadata templateMetadata;

  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  // usage of this should be replaced with environmentIdentifierList. A better type based api is needed.
  @Deprecated
  public String getEnvironmentIdentifier() {
    return environmentIdentifierList.get(0);
  }
  public List<String> getHealthSourceIdentifiers() {
    if (healthSourceIdentifiers == null) {
      return new ArrayList<>();
    }
    return healthSourceIdentifiers;
  }

  public List<String> getChangeSourceIdentifiers() {
    if (changeSourceIdentifiers == null) {
      return new ArrayList<>();
    }
    return changeSourceIdentifiers;
  }
  public List<String> getEnvironmentIdentifierList() {
    if (environmentIdentifierList == null) {
      return Collections.emptyList();
    }
    return environmentIdentifierList;
  }

  public List<NotificationRuleRef> getNotificationRuleRefs() {
    if (notificationRuleRefs == null) {
      return Collections.emptyList();
    }
    return notificationRuleRefs;
  }

  public String getTemplateIdentifier() {
    if (templateIdentifier == null && !Objects.isNull(templateMetadata)) {
      return templateMetadata.getTemplateIdentifier();
    }
    return templateIdentifier;
  }

  public String getTemplateVersionLabel() {
    if (templateVersionLabel == null && !Objects.isNull(templateMetadata)) {
      return templateMetadata.getVersionLabel();
    }
    return templateVersionLabel;
  }

  public boolean isTemplateByReference() {
    if (!Objects.isNull(templateMetadata)) {
      return templateMetadata.isTemplateByReference();
    }
    return false;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (MonitoredServiceKeys.nextNotificationIteration.equals(fieldName)) {
      return this.nextNotificationIteration;
    }
    if (MonitoredServiceKeys.nextErrorTrackingNotificationIteration.equals(fieldName)) {
      return this.nextErrorTrackingNotificationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (MonitoredServiceKeys.nextNotificationIteration.equals(fieldName)) {
      this.nextNotificationIteration = nextIteration;
      return;
    }
    if (MonitoredServiceKeys.nextErrorTrackingNotificationIteration.equals(fieldName)) {
      this.nextErrorTrackingNotificationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public static String getIdentifier(String serviceIdentifier, String envIdentifier) {
    return serviceIdentifier + "_" + envIdentifier;
  }
}
