/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1.webhook.bitbucket.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngtriggers.beans.source.v1.webhook.Conditions;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class BitbucketPushSpec implements BitbucketEventSpec {
  String connector;
  String repo;
  Conditions conditions;
  boolean abort_previous;

  @Override
  public String fetchConnectorRef() {
    return connector;
  }

  @Override
  public String fetchRepoName() {
    return repo;
  }

  @Override
  public GitEvent fetchEvent() {
    return BitbucketTriggerEvent.PUSH;
  }

  @Override
  public List<GitAction> fetchActions() {
    return emptyList();
  }

  @Override
  public List<TriggerEventDataCondition> fetchHeaderConditions() {
    return conditions.getHeader();
  }

  @Override
  public List<TriggerEventDataCondition> fetchPayloadConditions() {
    return conditions.getPayload();
  }

  @Override
  public String fetchJexlCondition() {
    return conditions.getJexl();
  }

  @Override
  public boolean fetchAutoAbortPreviousExecutions() {
    return abort_previous;
  }
}
