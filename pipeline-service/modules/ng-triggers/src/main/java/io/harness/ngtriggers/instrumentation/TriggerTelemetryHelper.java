/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.instrumentation;

import static io.harness.telemetry.helpers.InstrumentationConstants.ACCOUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.COUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.ORG;
import static io.harness.telemetry.helpers.InstrumentationConstants.PIPELINE_ID;
import static io.harness.telemetry.helpers.InstrumentationConstants.PROJECT;
import static io.harness.telemetry.helpers.InstrumentationConstants.TIME_TAKEN;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.BulkTriggersRequestDTO;
import io.harness.ngtriggers.beans.dto.BulkTriggersResponseDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ManifestTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.MultiRegionArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.telemetry.helpers.InstrumentationHelper;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class TriggerTelemetryHelper extends InstrumentationHelper {
  public static final String TRIGGER_TYPE = "trigger_type";
  public static final String TRIGGER_SUB_TYPE = "trigger_sub_type";
  public static final String TRIGGER_TOGGLE = "trigger_toggle";
  public static final String TRIGGER_CREATION_EVENT = "trigger_creation_event";
  public static final String BULK_TOGGLE_TRIGGERS_API = "bulk_toggle_triggers_api";

  public CompletableFuture<Void> sendBulkToggleTriggersApiEvent(String accountId,
      BulkTriggersRequestDTO bulkTriggersRequestDTO, BulkTriggersResponseDTO bulkTriggersResponseDTO, long timeTaken) {
    String orgId = null;
    String projectId = null;
    String pipelineId = null;
    String type = null;
    boolean enable = false;
    long modifiedCount = 0;

    // Filters and Data from the RequestBody
    if (bulkTriggersRequestDTO != null && bulkTriggersRequestDTO.getFilters() != null) {
      orgId = bulkTriggersRequestDTO.getFilters().getOrgIdentifier();
      projectId = bulkTriggersRequestDTO.getFilters().getProjectIdentifier();
      pipelineId = bulkTriggersRequestDTO.getFilters().getPipelineIdentifier();
      type = bulkTriggersRequestDTO.getFilters().getType();
    }

    if (bulkTriggersRequestDTO.getData() != null) {
      enable = bulkTriggersRequestDTO.getData().isEnable();
    }

    if (bulkTriggersResponseDTO != null) {
      modifiedCount = bulkTriggersResponseDTO.getCount();
    }

    return publishBulkToggleTriggersApiInfo(
        BULK_TOGGLE_TRIGGERS_API, accountId, orgId, projectId, pipelineId, type, enable, timeTaken, modifiedCount);
  }

  public CompletableFuture<Void> sendTriggersCreateEvent(
      NGTriggerEntity ngTriggerEntity, TriggerDetails triggerDetails) {
    String orgId = null;
    String projectId = null;
    String accountId = null;
    String type = null;
    String triggerSubtype = null;

    if (ngTriggerEntity != null && ngTriggerEntity.getType() != null) {
      orgId = ngTriggerEntity.getOrgIdentifier();
      projectId = ngTriggerEntity.getProjectIdentifier();
      accountId = ngTriggerEntity.getAccountId();
      type = ngTriggerEntity.getType().name();
      triggerSubtype = getTriggerSpecType(ngTriggerEntity, triggerDetails);
    }

    return publishTriggerCreateEvent(TRIGGER_CREATION_EVENT, accountId, orgId, projectId, type, triggerSubtype);
  }

  private CompletableFuture<Void> publishTriggerCreateEvent(String triggerCreationEvent, String accountId, String orgId,
      String projectId, String type, String triggerSubtype) {
    HashMap<String, Object> eventPropertiesMap = new HashMap<>();
    eventPropertiesMap.put(ACCOUNT, accountId);
    eventPropertiesMap.put(ORG, orgId);
    eventPropertiesMap.put(PROJECT, projectId);
    eventPropertiesMap.put(TRIGGER_TYPE, type);
    eventPropertiesMap.put(TRIGGER_SUB_TYPE, triggerSubtype);

    return sendEvent(triggerCreationEvent, accountId, eventPropertiesMap);
  }

  private String getTriggerSpecType(NGTriggerEntity ngTriggerEntity, TriggerDetails triggerDetails) {
    if (triggerDetails != null && triggerDetails.getNgTriggerConfigV2() != null
        && triggerDetails.getNgTriggerConfigV2().getSource() != null) {
      NGTriggerSourceV2 sourceV2 = triggerDetails.getNgTriggerConfigV2().getSource();
      switch (ngTriggerEntity.getType()) {
        case WEBHOOK:
          WebhookTriggerConfigV2 webhookTriggerConfig = (WebhookTriggerConfigV2) sourceV2.getSpec();
          return webhookTriggerConfig.getType().getValue();
        case ARTIFACT:
          ArtifactTriggerConfig artifactTriggerConfig = (ArtifactTriggerConfig) sourceV2.getSpec();
          return artifactTriggerConfig.getType().getValue();
        case MULTI_REGION_ARTIFACT:
          MultiRegionArtifactTriggerConfig multiRegionArtifactTriggerConfig =
              (MultiRegionArtifactTriggerConfig) sourceV2.getSpec();
          return multiRegionArtifactTriggerConfig.getType().getValue();
        case MANIFEST:
          ManifestTriggerConfig manifestTriggerConfig = (ManifestTriggerConfig) sourceV2.getSpec();
          return manifestTriggerConfig.getType().getValue();
        case SCHEDULED:
          ScheduledTriggerConfig scheduledTriggerConfig = (ScheduledTriggerConfig) sourceV2.getSpec();
          CronTriggerSpec spec = (CronTriggerSpec) scheduledTriggerConfig.getSpec();
          return spec.getType();
        default:
          return null;
      }
    }
    return null;
  }

  private CompletableFuture<Void> publishBulkToggleTriggersApiInfo(String eventName, String accountId, String orgId,
      String projectId, String pipelineId, String type, boolean enable, long timeTaken, long modifiedCount) {
    HashMap<String, Object> eventPropertiesMap = new HashMap<>();
    eventPropertiesMap.put(ACCOUNT, accountId);
    eventPropertiesMap.put(ORG, orgId);
    eventPropertiesMap.put(PROJECT, projectId);
    eventPropertiesMap.put(PIPELINE_ID, pipelineId);
    eventPropertiesMap.put(TRIGGER_TYPE, type);
    eventPropertiesMap.put(TRIGGER_TOGGLE, enable);
    eventPropertiesMap.put(TIME_TAKEN, timeTaken);
    eventPropertiesMap.put(COUNT, modifiedCount);

    return sendEvent(eventName, accountId, eventPropertiesMap);
  }
}
