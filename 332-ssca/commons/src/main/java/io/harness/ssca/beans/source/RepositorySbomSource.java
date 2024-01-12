/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.source;

import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepositorySbomSource implements SbomSourceSpec {
  @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> url;
  @ApiModelProperty(dataType = STRING_CLASSPATH) @NotEmpty ParameterField<String> path;
  @NotNull @JsonProperty("variant_type") private RepoSbomVariantType variantType;
  @ApiModelProperty(dataType = STRING_CLASSPATH) @NotEmpty ParameterField<String> variant;
  @JsonProperty("cloned_codebase")
  @ApiModelProperty(dataType = STRING_CLASSPATH)
  @NotEmpty
  ParameterField<String> clonedCodebase;
}
