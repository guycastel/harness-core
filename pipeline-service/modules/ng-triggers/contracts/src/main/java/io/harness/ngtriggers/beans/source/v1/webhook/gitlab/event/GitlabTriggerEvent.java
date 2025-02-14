/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1.webhook.gitlab.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.MR_COMMENT_EVENT_TYPE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.MR_EVENT_TYPE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.PUSH_EVENT_TYPE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(PIPELINE)
public enum GitlabTriggerEvent implements GitEvent {
  @JsonProperty(MR_EVENT_TYPE) MERGE_REQUEST(MR_EVENT_TYPE),
  @JsonProperty(PUSH_EVENT_TYPE) PUSH(PUSH_EVENT_TYPE),
  @JsonProperty(MR_COMMENT_EVENT_TYPE) MR_COMMENT(MR_COMMENT_EVENT_TYPE);

  private String value;

  GitlabTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
