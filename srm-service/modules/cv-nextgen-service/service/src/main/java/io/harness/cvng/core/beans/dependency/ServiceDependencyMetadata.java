/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.dependency;

import io.harness.cvng.beans.change.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants(innerTypeName = "ServiceDependencyMetadataKeys")
@NoArgsConstructor
@SuperBuilder
@JsonSubTypes({ @JsonSubTypes.Type(value = KubernetesDependencyMetadata.class, name = "KUBERNETES") })
public abstract class ServiceDependencyMetadata {
  private DependencyMetadataType type;

  public abstract Set<ChangeSourceType> getSupportedChangeSourceTypes();
}
