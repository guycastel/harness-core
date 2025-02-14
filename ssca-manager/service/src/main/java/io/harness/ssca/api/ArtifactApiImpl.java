/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.spec.server.ssca.v1.ArtifactApi;
import io.harness.spec.server.ssca.v1.model.ArtifactComponentViewRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactComponentViewResponse;
import io.harness.spec.server.ssca.v1.model.ArtifactDeploymentViewRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactDeploymentViewResponse;
import io.harness.spec.server.ssca.v1.model.ArtifactDetailResponse;
import io.harness.spec.server.ssca.v1.model.ArtifactListingRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactListingResponse;
import io.harness.spec.server.ssca.v1.model.ArtifactListingResponseV2;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.utils.PageResponseUtils;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.core.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class ArtifactApiImpl implements ArtifactApi {
  @Inject ArtifactService artifactService;

  @Override
  public Response getArtifactDetailComponentView(String org, String project, String artifact, String tag,
      @Valid ArtifactComponentViewRequestBody body, String harnessAccount, @Min(1L) @Max(1000L) Integer limit,
      String order, @Min(0L) Integer page, String sort) {
    sort = ArtifactApiUtils.getSortFieldMapping(sort);
    Pageable pageable = PageResponseUtils.getPageable(page, limit, sort, order);
    Page<ArtifactComponentViewResponse> artifactComponentViewResponses =
        artifactService.getArtifactComponentView(harnessAccount, org, project, artifact, tag, body, pageable);
    return PageResponseUtils.getPagedResponse(artifactComponentViewResponses);
  }

  @Override
  public Response getArtifactDetailDeploymentView(String org, String project, String artifact, String tag,
      @Valid ArtifactDeploymentViewRequestBody body, String harnessAccount, @Min(1L) @Max(1000L) Integer limit,
      String order, @Min(0L) Integer page, String sort) {
    sort = ArtifactApiUtils.getSortFieldMapping(sort);
    Pageable pageable = PageResponseUtils.getPageable(page, limit, sort, order);
    Page<ArtifactDeploymentViewResponse> artifactDeploymentViewResponses =
        artifactService.getArtifactDeploymentView(harnessAccount, org, project, artifact, tag, body, pageable);
    return PageResponseUtils.getPagedResponse(artifactDeploymentViewResponses);
  }

  @Override
  public Response getArtifactDetails(String org, String project, String artifact, String tag, String harnessAccount) {
    ArtifactDetailResponse response = artifactService.getArtifactDetails(harnessAccount, org, project, artifact, tag);
    return Response.ok().entity(response).build();
  }

  @Override
  public Response listArtifacts(String org, String project, @Valid ArtifactListingRequestBody body,
      String harnessAccount, @Min(1L) @Max(1000L) Integer limit, String order, @Min(0L) Integer page, String sort) {
    sort = ArtifactApiUtils.getSortFieldMapping(sort);
    Pageable pageable = PageResponseUtils.getPageable(page, limit, sort, order);
    Page<ArtifactListingResponseV2> artifactEntities =
        artifactService.listArtifacts(harnessAccount, org, project, body, pageable, "image");
    Page<ArtifactListingResponse> artifactEntitiesV1 = ArtifactApiUtils.toArtifactListingResponseList(artifactEntities);
    return PageResponseUtils.getPagedResponse(artifactEntitiesV1);
  }

  @Override
  public Response listLatestArtifacts(String org, String project, String harnessAccount,
      @Min(1L) @Max(1000L) Integer limit, String order, @Min(0L) Integer page, String sort) {
    sort = ArtifactApiUtils.getSortFieldMapping(sort);
    Pageable pageable = PageResponseUtils.getPageable(page, limit, sort, order);
    Page<ArtifactListingResponseV2> artifactEntities =
        artifactService.listLatestArtifacts(harnessAccount, org, project, pageable, "image");
    Page<ArtifactListingResponse> artifactEntitiesV1 = ArtifactApiUtils.toArtifactListingResponseList(artifactEntities);
    return PageResponseUtils.getPagedResponse(artifactEntitiesV1);
  }
}
