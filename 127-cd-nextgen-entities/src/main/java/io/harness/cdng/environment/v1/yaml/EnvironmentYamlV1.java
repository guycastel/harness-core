/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.environment.v1.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.infra.v1.yaml.InfraDefinitionYamlV1;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@Value
@Builder
@OwnedBy(CDC)
@RecasterAlias("io.harness.cdng.environment.v1.yaml.EnvironmentYamlV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentYamlV1 implements Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME) String uuid;

  @Pattern(regexp = NGRegexValidatorConstants.NON_EMPTY_STRING_PATTERN) ParameterField<String> ref;

  ParameterField<Map<String, Object>> inputs;

  @NotNull ParameterField<List<InfraDefinitionYamlV1>> infra;
}
