/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.spec.server.ssca.v1.model.ArtifactListingResponse;
import io.harness.spec.server.ssca.v1.model.ArtifactListingResponseScorecard;
import io.harness.spec.server.ssca.v1.model.ArtifactListingResponseV2;
import io.harness.spec.server.ssca.v1.model.ArtifactListingResponseV2Deployment;
import io.harness.ssca.entities.CdInstanceSummary.CdInstanceSummaryKeys;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys;
import io.harness.ssca.entities.artifact.ArtifactEntity.ArtifactEntityKeys;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Slf4j
public class ArtifactApiUtils {
  public static String getSortFieldMapping(String field) {
    switch (field) {
      case "name":
        return ArtifactEntityKeys.name;
      case "updated":
        return ArtifactEntityKeys.lastUpdatedAt;
      case "env_name":
        return CdInstanceSummaryKeys.envName;
      case "env_type":
        return CdInstanceSummaryKeys.envType;
      case "package_name":
        return NormalizedSBOMEntityKeys.packageName.toLowerCase();
      case "package_supplier":
        return NormalizedSBOMEntityKeys.packageOriginatorName.toLowerCase();
      default:
        log.info(String.format("Mapping not found for field: %s", field));
    }
    return field;
  }

  public static Page<ArtifactListingResponse> toArtifactListingResponseList(
      Page<ArtifactListingResponseV2> responseV2Page) {
    List<ArtifactListingResponse> responseList = responseV2Page.getContent()
                                                     .stream()
                                                     .map(ArtifactApiUtils::toArtifactListingResponse)
                                                     .collect(Collectors.toList());
    return new PageImpl<>(responseList,
        PageRequest.of(responseV2Page.getNumber(), responseV2Page.getSize(), responseV2Page.getSort()),
        responseV2Page.getTotalElements());
  }

  private static ArtifactListingResponse toArtifactListingResponse(ArtifactListingResponseV2 responseV2) {
    return new ArtifactListingResponse()
        .id(responseV2.getId())
        .name(responseV2.getName())
        .tag(responseV2.getVariant().getValue())
        .url(responseV2.getUrl())
        .componentsCount(responseV2.getComponentsCount())
        .allowListViolationCount(responseV2.getPolicyEnforcement().getAllowListViolationCount())
        .denyListViolationCount(responseV2.getPolicyEnforcement().getDenyListViolationCount())
        .enforcementId(responseV2.getPolicyEnforcement().getId())
        .activity(responseV2.getDeployment().getActivity() == ArtifactListingResponseV2Deployment.ActivityEnum.DEPLOYED
                ? ArtifactListingResponse.ActivityEnum.DEPLOYED
                : ArtifactListingResponse.ActivityEnum.GENERATED)
        .updated(responseV2.getUpdated())
        .prodEnvCount(responseV2.getDeployment().getProdEnvCount())
        .nonProdEnvCount(responseV2.getDeployment().getNonProdEnvCount())
        .orchestrationId(responseV2.getOrchestration().getId())
        .buildPipelineId(responseV2.getOrchestration().getPipelineId())
        .buildPipelineExecutionId(responseV2.getOrchestration().getPipelineExecutionId())
        .baseline(responseV2.isBaseline())
        .scorecard(new ArtifactListingResponseScorecard()
                       .avgScore(responseV2.getScorecard().getAvgScore())
                       .maxScore(responseV2.getScorecard().getMaxScore()));
  }
}
