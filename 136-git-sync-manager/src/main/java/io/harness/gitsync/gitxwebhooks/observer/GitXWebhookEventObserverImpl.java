/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.gitsync.gitxwebhooks.observer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.gitsync.common.beans.GitXWebhookEventStatus;
import io.harness.gitsync.gitxwebhooks.dtos.GitXWebhookEventUpdateInfo;
import io.harness.gitsync.gitxwebhooks.helper.GitXWebhookTriggerHelper;
import io.harness.gitsync.gitxwebhooks.loggers.GitXWebhookEventLogContext;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@Slf4j
@OwnedBy(PIPELINE)
public class GitXWebhookEventObserverImpl implements GitXWebhookEventUpdateObserver {
  @Inject private GitXWebhookTriggerHelper gitXWebhookTriggerHelper;

  @Override
  public void onGitXWebhookEventUpdate(GitXWebhookEventUpdateInfo gitXWebhookEventUpdateInfo) {
    try (GitXWebhookEventLogContext context =
             new GitXWebhookEventLogContext(gitXWebhookEventUpdateInfo.getWebhookDTO())) {
      if (shouldStartTriggerExecution(gitXWebhookEventUpdateInfo.getEventStatus())) {
        try {
          log.info(String.format(
              "Starting the trigger execution for event status %s", gitXWebhookEventUpdateInfo.getEventStatus()));
          gitXWebhookTriggerHelper.startTriggerExecution(gitXWebhookEventUpdateInfo.getWebhookDTO());
        } catch (Exception exception) {
          log.error("Faced exception while sequentially executing the trigger for the event: {} ",
              gitXWebhookEventUpdateInfo.getWebhookDTO().getEventId(), exception);
        }
      }
    }
  }

  private boolean shouldStartTriggerExecution(String eventStatus) {
    return getAllowedTriggerExecutionEventStatus().contains(eventStatus);
  }

  private List<String> getAllowedTriggerExecutionEventStatus() {
    List<String> allowedTriggerExecutionEventStatus = new ArrayList<>();
    allowedTriggerExecutionEventStatus.add(GitXWebhookEventStatus.SUCCESSFUL.name());
    allowedTriggerExecutionEventStatus.add(GitXWebhookEventStatus.FAILED.name());
    allowedTriggerExecutionEventStatus.add(GitXWebhookEventStatus.SKIPPED.name());
    return allowedTriggerExecutionEventStatus;
  }
}
