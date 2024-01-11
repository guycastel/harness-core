/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.exception.InvalidArgumentsException;
import io.harness.spec.server.ssca.v1.ArtifactV2Api;
import io.harness.spec.server.ssca.v1.model.ArtifactListingRequestBody;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.utils.PageResponseUtils;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import org.springframework.data.domain.Pageable;

public class ArtifactV2ApiImpl implements ArtifactV2Api {
  @Inject ArtifactService artifactService;
  @Override
  public Response artifactList(@NotNull String type, String org, String project, @Valid ArtifactListingRequestBody body,
      String harnessAccount, @Min(1L) @Max(1000L) Integer limit, String order, @Min(0L) Integer page, String sort,
      String viewMode) {
    sort = ArtifactApiUtils.getSortFieldMapping(sort);
    Pageable pageable = PageResponseUtils.getPageable(page, limit, sort, order);
    switch (viewMode) {
      case "all":
        return PageResponseUtils.getPagedResponse(
            artifactService.listArtifacts(harnessAccount, org, project, body, pageable, type));
      case "latest":
        return PageResponseUtils.getPagedResponse(
            artifactService.listLatestArtifacts(harnessAccount, org, project, pageable, type));
      default:
        throw new InvalidArgumentsException(String.format("Invalid value for view mode [%s]", viewMode));
    }
  }
}
