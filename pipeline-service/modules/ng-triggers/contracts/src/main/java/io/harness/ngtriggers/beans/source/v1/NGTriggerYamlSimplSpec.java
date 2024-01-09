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
import io.harness.ngtriggers.beans.source.artifact.ArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ManifestTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.MultiRegionArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.v1.scheduled.ScheduledTriggerYamlSimplConfig;
import io.harness.ngtriggers.beans.source.v1.webhook.WebhookTriggerYamlSimplConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ScheduledTriggerYamlSimplConfig.class, name = YamlSimplConstants.SCHEDULED_TYPE)
  , @JsonSubTypes.Type(value = WebhookTriggerYamlSimplConfig.class, name = YamlSimplConstants.WEBHOOK_TYPE),
      @JsonSubTypes.Type(value = ArtifactTriggerConfig.class, name = YamlSimplConstants.ARTIFACT_TYPE),
      @JsonSubTypes.Type(value = ManifestTriggerConfig.class, name = YamlSimplConstants.MANIFEST_TYPE),
      @JsonSubTypes.Type(
          value = MultiRegionArtifactTriggerConfig.class, name = YamlSimplConstants.MULTI_REGION_ARTIFACT_TYPE)
})
@OwnedBy(PIPELINE)
public interface NGTriggerYamlSimplSpec {}
