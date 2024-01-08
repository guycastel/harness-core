/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.schema.inputs.beans;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.yaml.individualschema.InputFieldMetadata;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Data
@Builder
@AllArgsConstructor
public class InputMetadata {
  DependencyDetails dependencyDetails;
  InputDetails inputDetails;
  List<InputFieldMetadata> requiredFieldMetadata;

  public InputMetadata() {
    dependencyDetails = new DependencyDetails();
  }

  @Data
  @Builder
  @AllArgsConstructor
  public static class InputDetails {
    String inputType;
    String entityGroup;
    String entityType;
    String fqnFromEntityRoot;
  }

  public void setInputDetails(String inputType, String entityGroup, String entityType, String fqnFromEntityRoot) {
    inputDetails = InputDetails.builder()
                       .inputType(inputType)
                       .entityGroup(entityGroup)
                       .entityType(entityType)
                       .fqnFromEntityRoot(fqnFromEntityRoot)
                       .build();
  }

  public void addDependencyDetails(DependencyDetails dependencyDetails) {
    if (this.dependencyDetails == null) {
      this.dependencyDetails = new DependencyDetails();
    }
    this.dependencyDetails.addRuntimeInputDependency(dependencyDetails.getRuntimeInputDependencyDetailsList());
    this.dependencyDetails.addFixedValueDependency(dependencyDetails.getFixedValueDependencyDetailsList());
  }
}
