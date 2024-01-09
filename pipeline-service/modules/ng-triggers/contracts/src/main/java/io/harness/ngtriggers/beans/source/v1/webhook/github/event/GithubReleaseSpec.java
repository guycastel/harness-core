/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1.webhook.github.event;

import static io.harness.annotations.dev.HarnessTeam.SPG;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent.RELEASE;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngtriggers.beans.source.v1.webhook.Conditions;
import io.harness.ngtriggers.beans.source.v1.webhook.github.action.GithubReleaseAction;
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
@OwnedBy(SPG)
public class GithubReleaseSpec implements GithubEventSpec {
  String connector;
  String repo;
  List<GithubReleaseAction> actions;
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
    return RELEASE;
  }

  @Override
  public List<GitAction> fetchActions() {
    if (isEmpty(actions)) {
      return emptyList();
    }

    return actions.stream().collect(toList());
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
