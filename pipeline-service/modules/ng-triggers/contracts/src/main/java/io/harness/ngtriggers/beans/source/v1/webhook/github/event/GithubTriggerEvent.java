/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1.webhook.github.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.DELETE_EVENT_TYPE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.ISSUE_COMMENT_EVENT_TYPE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.PR_EVENT_TYPE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.PUSH_EVENT_TYPE;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.RELEASE_EVENT_TYPE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(PIPELINE)
public enum GithubTriggerEvent implements GitEvent {
  @JsonProperty(PR_EVENT_TYPE) PULL_REQUEST(PR_EVENT_TYPE),
  @JsonProperty(PUSH_EVENT_TYPE) PUSH(PUSH_EVENT_TYPE),
  @JsonProperty(ISSUE_COMMENT_EVENT_TYPE) ISSUE_COMMENT(ISSUE_COMMENT_EVENT_TYPE),
  @JsonProperty(RELEASE_EVENT_TYPE) RELEASE(RELEASE_EVENT_TYPE),
  @JsonProperty(DELETE_EVENT_TYPE) DELETE(DELETE_EVENT_TYPE);
  private String value;

  GithubTriggerEvent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
