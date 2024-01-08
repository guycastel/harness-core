/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities.exemption;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.ng.DbAliases;
import io.harness.ssca.entities.OperatorEntity;

import dev.morphia.annotations.Entity;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder(toBuilder = true)
@StoreIn(DbAliases.SSCA)
@Entity(value = "exemptions", noClassnameStored = true)
@Document("exemptions")
@TypeAlias("exemptions")
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "ExemptionKeys")
// TODO: Add indexes
@OwnedBy(HarnessTeam.SSCA)
public class Exemption implements PersistentIterable, PersistentRegularIterable {
  @Id String uuid;
  @NotBlank String componentName;
  String componentVersion;
  OperatorEntity versionOperator;
  String reason;
  @NotNull ExemptionDuration exemptionDuration;
  @NotNull ExemptionStatus exemptionStatus;
  @NotNull ExemptionInitiator exemptionInitiator;
  String artifactId;
  @NotBlank String accountId;
  @NotBlank String orgIdentifier;
  @NotBlank String projectIdentifier;
  @NotBlank String createdBy;
  String reviewedBy;
  @NotBlank String updatedBy;
  String reviewComment;
  Long createdAt;
  @LastModifiedDate Long updatedAt;
  Long validUntil;
  Long reviewedAt;
  Long iteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (fieldName.equals(ExemptionKeys.iteration)) {
      return Objects.isNull(iteration) ? 0L : iteration;
    } else {
      throw new IllegalArgumentException("Invalid fieldName " + fieldName);
    }
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (fieldName.equals(ExemptionKeys.iteration)) {
      iteration = nextIteration;
    } else {
      throw new IllegalArgumentException("Invalid fieldName " + fieldName);
    }
  }

  @Data
  @Builder
  public static class ExemptionDuration {
    boolean alwaysExempt;
    Integer days;
  }

  @Data
  @Builder(toBuilder = true)
  public static class ExemptionInitiator {
    String projectId;
    String artifactId;
    String enforcementId;
  }
  public enum ExemptionStatus { PENDING, APPROVED, REJECTED, EXPIRED }
}
