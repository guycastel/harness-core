/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.entities.subscriptions;

import static io.harness.annotations.dev.HarnessTeam.CHAOS;

import io.harness.annotations.ChangeDataCapture;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import dev.morphia.annotations.Entity;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CHAOS)
@Data
@Builder
@FieldNameConstants(innerTypeName = "ChaosLinuxInfrastructuresKeys", asEnum = true)
@StoreIn(DbAliases.CHAOS)
@Entity(value = "linuxInfrastructures", noClassnameStored = true)
@Document("linuxInfrastructures")
@TypeAlias("linuxInfrastructures")
@ChangeDataCapture(table = "linux_infrastructures", dataStore = "chaos", fields = {}, handler = "")
@ChangeDataCapture(
    table = "linux_infrastructures__tags", dataStore = "chaos", fields = {}, handler = "ChaosExperimentsTags")
public class ChaosLinuxInfrastructures implements PersistentEntity {
  @Id @dev.morphia.annotations.Id String id;
  String account_id;
  String org_id;
  String project_id;
  String name;
  String description;
  List<String> tags;
  @LastModifiedDate Long updated_at;
  @CreatedDate Long created_at;
  String created_by;
  String updated_by;
  boolean is_removed;
  String infra_id;
  String environment_id;
  String hostname;
  Long last_heartbeat;
  boolean is_registered;
  boolean is_infra_confirmed;
  boolean is_active;
  Long start_time;
  String version;
}
