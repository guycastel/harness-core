/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@JsonTypeName("spec")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class NGTriggerYamlSimplSource {
  private static final String INTERVAL_PATTERN = "(((([1-9])+\\d*[mh])+(\\s/?\\d+[mh])*)|(^$)|(0))$";
  NGTriggerYamlSimplType type;

  @Pattern(regexp = INTERVAL_PATTERN) String interval;

  // Webhook id is created during auto-registration process but in some cases where customer cannot reach harness, user
  // will manually input it
  String webhook;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) NGTriggerYamlSimplSpec spec;

  @Builder
  public NGTriggerYamlSimplSource(
      NGTriggerYamlSimplType type, NGTriggerYamlSimplSpec spec, String interval, String webhook) {
    this.type = type;
    this.spec = spec;
    this.interval = interval;
    this.webhook = webhook;
  }
}
