/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.triggers.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.NGTriggerType.SCHEDULED;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityBuilder;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTypeSpecWrapper;
import io.harness.ngtriggers.beans.source.artifact.ManifestTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.MultiRegionArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.beans.source.v1.NGTriggerYamlSimplSource;
import io.harness.ngtriggers.beans.source.v1.NGTriggerYamlSimplType;
import io.harness.ngtriggers.beans.source.v1.artifact.ArtifactType;
import io.harness.ngtriggers.beans.source.v1.scheduled.ScheduledTriggerYamlSimplConfig;
import io.harness.ngtriggers.beans.source.v1.webhook.WebhookTriggerYamlSimplConfig;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.target.TargetType;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YamlUtils;
import io.harness.spec.server.pipeline.v1.model.TriggerBody;
import io.harness.spec.server.pipeline.v1.model.TriggerGetResponseBody;
import io.harness.spec.server.pipeline.v1.model.TriggerRequestBody;
import io.harness.spec.server.pipeline.v1.model.TriggerResponseBody;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class NGTriggerApiUtils {
  io.harness.ngtriggers.mapper.NGTriggerElementMapper ngTriggerElementMapper;
  NGWebhookTriggerApiUtils ngWebhookTriggerApiUtils;
  NGArtifactTriggerApiUtils ngArtifactTriggerApiUtils;
  NGManifestTriggerApiUtils ngManifestTriggerApiUtils;

  public TriggerDetails toTriggerDetails(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TriggerRequestBody body, String pipeline) {
    NGTriggerSourceV2 source = toNGTriggerSourceV2(body.getYaml());
    NGTriggerEntity ngTriggerEntity =
        toTriggerEntity(accountIdentifier, orgIdentifier, projectIdentifier, body, pipeline, source);
    return TriggerDetails.builder()
        .ngTriggerConfigV2(toNGTriggerConfigV2(pipeline, orgIdentifier, projectIdentifier, body))
        .ngTriggerEntity(ngTriggerEntity)
        .build();
  }

  public NGTriggerEntity toTriggerEntity(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TriggerRequestBody body, String pipeline, NGTriggerSourceV2 source) {
    NGTriggerEntityBuilder entityBuilder =
        NGTriggerEntity.builder()
            .name(body.getName())
            .identifier(body.getIdentifier())
            .description(body.getDescription())
            .harnessVersion(HarnessYamlVersion.V1)
            .accountId(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .targetIdentifier(pipeline)
            .targetType(TargetType.PIPELINE)
            .enabled(body.isEnabled())
            .yaml(body.getYaml())
            .type(source.getType())
            .pollInterval(source.getPollInterval())
            .webhookId(source.getWebhookId())
            .metadata(ngTriggerElementMapper.toMetadata(source, accountIdentifier))
            .withServiceV2(true)
            .tags(TagMapper.convertToList(body.getTags()))
            .encryptedWebhookSecretIdentifier(body.getEncryptedWebhookSecretIdentifier())
            .stagesToExecute(body.getStagesToExecute())
            .tags(TagMapper.convertToList(body.getTags()));

    if (source.getType() == SCHEDULED) {
      entityBuilder.nextIterations(new ArrayList<>());
    }
    NGTriggerEntity entity = entityBuilder.build();
    if (source.getType() == SCHEDULED) {
      List<Long> nextIterations = entity.recalculateNextIterations("unused", true, 0);
      if (!nextIterations.isEmpty()) {
        entity.setNextIterations(nextIterations);
      }
    }
    return entity;
  }

  NGTriggerType toNGTriggerType(NGTriggerYamlSimplType typeEnum) {
    switch (typeEnum) {
      case WEBHOOK:
        return NGTriggerType.WEBHOOK;
      case MANIFEST:
        return NGTriggerType.MANIFEST;
      case SCHEDULED:
        return SCHEDULED;
      case ARTIFACT:
        return NGTriggerType.ARTIFACT;
      case MULTI_REGION_ARTIFACT:
        return NGTriggerType.MULTI_REGION_ARTIFACT;
      default:
        throw new InvalidRequestException(String.format("NGTrigger not supported for type: %s", typeEnum));
    }
  }

  NGTriggerConfigV2 toNGTriggerConfigV2(String pipeline, String org, String project, TriggerRequestBody body) {
    return NGTriggerConfigV2.builder()
        .pipelineIdentifier(pipeline)
        .identifier(body.getIdentifier())
        .projectIdentifier(project)
        .orgIdentifier(org)
        .encryptedWebhookSecretIdentifier(body.getEncryptedWebhookSecretIdentifier())
        .enabled(body.isEnabled())
        .description(body.getDescription())
        .inputYaml(body.getInputs())
        .inputSetRefs(body.getInputSetRefs())
        .name(body.getName())
        .pipelineBranchName(body.getPipelineBranchName())
        .tags(body.getTags())
        .stagesToExecute(body.getStagesToExecute())
        .source(toNGTriggerSourceV2(body.getYaml()))
        .build();
  }

  public NGTriggerSourceV2 toNGTriggerSourceV2(String yaml) {
    try {
      NGTriggerYamlSimplSource source = YamlUtils.read(yaml, NGTriggerYamlSimplSource.class);
      return NGTriggerSourceV2.builder()
          .pollInterval(source.getInterval())
          .webhookId(source.getWebhook())
          .type(toNGTriggerType(source.getType()))
          .spec(toNGTriggerSpecV2(source))
          .build();
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage());
    }
  }

  NGTriggerSpecV2 toNGTriggerSpecV2(NGTriggerYamlSimplSource source) {
    switch (source.getType()) {
      case SCHEDULED:
        ScheduledTriggerYamlSimplConfig spec = (ScheduledTriggerYamlSimplConfig) source.getSpec();
        io.harness.ngtriggers.beans.source.v1.scheduled.CronTriggerSpec cronTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.scheduled.CronTriggerSpec) spec.getSpec();
        return ScheduledTriggerConfig.builder()
            .type(spec.getType())
            .spec(CronTriggerSpec.builder()
                      .type(cronTriggerSpec.getType())
                      .expression(cronTriggerSpec.getExpression())
                      .build())
            .build();
      case WEBHOOK:
        WebhookTriggerYamlSimplConfig webhookSpec = (WebhookTriggerYamlSimplConfig) source.getSpec();
        return WebhookTriggerConfigV2.builder()
            .type(ngWebhookTriggerApiUtils.toWebhookTriggerType(webhookSpec.getType()))
            .spec(ngWebhookTriggerApiUtils.toWebhookTriggerSpec(webhookSpec))
            .build();
      case ARTIFACT:
        io.harness.ngtriggers.beans.source.v1.artifact.ArtifactTriggerConfig artifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.ArtifactTriggerConfig) source.getSpec();
        return ArtifactTriggerConfig.builder()
            .type(ngArtifactTriggerApiUtils.toArtifactTriggerType(artifactTriggerSpec.getType()))
            .spec(ngArtifactTriggerApiUtils.toArtifactTypeSpec(
                artifactTriggerSpec.getSpec(), artifactTriggerSpec.getType()))
            .build();
      case MANIFEST:
        io.harness.ngtriggers.beans.source.v1.artifact.ManifestTriggerConfig manifestTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.ManifestTriggerConfig) source.getSpec();
        return ManifestTriggerConfig.builder()
            .type(ngManifestTriggerApiUtils.toManifestTriggerType(manifestTriggerSpec.getType()))
            .spec(ngManifestTriggerApiUtils.toManifestTypeSpec(manifestTriggerSpec))
            .build();
      case MULTI_REGION_ARTIFACT:
        io.harness.ngtriggers.beans.source.v1.artifact.MultiRegionArtifactTriggerConfig multiRegionArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.MultiRegionArtifactTriggerConfig) source.getSpec();
        return MultiRegionArtifactTriggerConfig.builder()
            .eventConditions(multiRegionArtifactTriggerSpec.getConditions().getEvent())
            .sources(
                multiRegionArtifactTriggerSpec.getSources()
                    .stream()
                    .map(artifactTypeSpecWrapper
                        -> toArtifactTypeSpecWrapper(artifactTypeSpecWrapper, multiRegionArtifactTriggerSpec.getType()))
                    .collect(Collectors.toList()))
            .jexlCondition(multiRegionArtifactTriggerSpec.getConditions().getJexl())
            .metaDataConditions(multiRegionArtifactTriggerSpec.getConditions().getMetadata())
            .type(ngArtifactTriggerApiUtils.toArtifactTriggerType(multiRegionArtifactTriggerSpec.getType()))
            .build();
      default:
        throw new InvalidRequestException("Type " + source.getType().toString() + " is invalid");
    }
  }

  ArtifactTypeSpecWrapper toArtifactTypeSpecWrapper(
      io.harness.ngtriggers.beans.source.v1.artifact.ArtifactTypeSpecWrapper artifactTypeSpecWrapper,
      ArtifactType artifactType) {
    return ArtifactTypeSpecWrapper.builder()
        .spec(ngArtifactTriggerApiUtils.toArtifactTypeSpec(artifactTypeSpecWrapper.getSpec(), artifactType))
        .build();
  }

  public TriggerResponseBody toResponseDTO(NGTriggerEntity triggerEntity) {
    TriggerResponseBody responseBody = new TriggerResponseBody();
    responseBody.setIdentifier(triggerEntity.getIdentifier());
    return responseBody;
  }

  public TriggerGetResponseBody toGetResponseDTO(NGTriggerEntity triggerEntity) {
    TriggerGetResponseBody responseBody = new TriggerGetResponseBody();
    responseBody.setIdentifier(triggerEntity.getIdentifier());
    responseBody.setTrigger(toTriggerBody(triggerEntity));
    responseBody.setDescription(triggerEntity.getDescription());
    responseBody.setName(triggerEntity.getName());
    responseBody.setOrg(triggerEntity.getOrgIdentifier());
    responseBody.setPipeline(triggerEntity.getTargetIdentifier());
    responseBody.setProject(triggerEntity.getProjectIdentifier());
    return responseBody;
  }

  public TriggerBody toTriggerBody(NGTriggerEntity triggerEntity) {
    TriggerBody triggerBody = new TriggerBody();
    triggerBody.setEnabled(triggerEntity.getEnabled());
    triggerBody.setEncryptedWebhookSecretIdentifier(triggerEntity.getEncryptedWebhookSecretIdentifier());
    triggerBody.setInputSetRefs(triggerEntity.getTriggerConfigWrapper().getInputSetRefs());
    triggerBody.setInputs(triggerEntity.getTriggerConfigWrapper().getInputYaml());
    triggerBody.setPipelineBranchName(triggerEntity.getTriggerConfigWrapper().getPipelineBranchName());
    triggerBody.setStagesToExecute(triggerEntity.getStagesToExecute());
    triggerBody.setTags(TagMapper.convertToMap(triggerEntity.getTags()));
    triggerBody.setYaml(triggerEntity.getYaml());
    return triggerBody;
  }
}
