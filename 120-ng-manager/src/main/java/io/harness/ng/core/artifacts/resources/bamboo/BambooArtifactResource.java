/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.artifacts.resources.bamboo;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.artifact.resources.bamboo.dtos.BambooPlanKeysDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.ng.core.artifacts.resources.util.BambooResourceUtils;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import software.wings.helpers.ext.jenkins.BuildDetails;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ARTIFACTS})
@OwnedBy(CDC)
@Api("artifacts")
@Path("/artifacts/bamboo")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class BambooArtifactResource {
  private final BambooResourceUtils bambooResourceUtils;

  @POST
  @Path("plans")
  @ApiOperation(value = "Gets Plan Keys for Bamboo", nickname = "getPlansKey")
  public ResponseDTO<BambooPlanKeysDTO> getPlansKey(@QueryParam("connectorRef") String bambooConnectorIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, @QueryParam(NGCommonEntityConstants.FQN_PATH) String fqnPath,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceRef, String runtimeInputYaml) {
    BambooPlanKeysDTO buildDetails =
        bambooResourceUtils.getBambooPlanKeys(bambooConnectorIdentifier, accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, gitEntityBasicInfo, fqnPath, serviceRef, runtimeInputYaml);
    return ResponseDTO.newResponse(buildDetails);
  }

  @POST
  @Path("/paths")
  @ApiOperation(value = "Get Artifact Paths for Bamboo", nickname = "getArtifactPathsForBamboo")
  public ResponseDTO<List<String>> getArtifactPaths(@QueryParam("connectorRef") String bambooConnectorIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.PLAN_NAME) String planName,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, @QueryParam(NGCommonEntityConstants.FQN_PATH) String fqnPath,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceRef, String runtimeInputYaml) {
    List<String> artifactPaths =
        bambooResourceUtils.getBambooArtifactPaths(bambooConnectorIdentifier, accountId, orgIdentifier,
            projectIdentifier, pipelineIdentifier, planName, gitEntityBasicInfo, fqnPath, serviceRef, runtimeInputYaml);
    return ResponseDTO.newResponse(artifactPaths);
  }

  @POST
  @Path("/builds")
  @ApiOperation(value = "Gets Builds details for Bamboo", nickname = "getBuildsForBamboo")
  public ResponseDTO<List<BuildDetails>> getBuilds(@QueryParam("connectorRef") String bambooConnectorIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.PLAN_NAME) String planName,
      @QueryParam(NGCommonEntityConstants.ARTIFACT_PATH) List<String> artifactPath,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo, @QueryParam(NGCommonEntityConstants.FQN_PATH) String fqnPath,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceRef, String runtimeInputYaml) {
    List<BuildDetails> artifactPaths = bambooResourceUtils.getBambooArtifactBuildDetails(bambooConnectorIdentifier,
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, planName, artifactPath, gitEntityBasicInfo,
        fqnPath, serviceRef, runtimeInputYaml);
    return ResponseDTO.newResponse(artifactPaths);
  }
}
