/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.resource;

import static io.harness.NGDateUtils.getNumberOfDays;
import static io.harness.NGDateUtils.getStartTimeOfPreviousInterval;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cd.NGServiceConstants;
import io.harness.cdng.service.beans.CustomSequenceDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.models.InstanceDetailGroupedByPipelineExecutionList;
import io.harness.models.InstanceDetailsByBuildId;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeAndServiceId;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.dashboard.DashboardExecutionStatusInfo;
import io.harness.ng.core.dashboard.DeploymentsInfo;
import io.harness.ng.core.dashboard.ServiceDeployments;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.beans.EnvironmentFilterPropertiesDTO;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.service.entity.ServiceSequence;
import io.harness.ng.core.utils.GitXUtils;
import io.harness.ng.overview.dto.ActiveServiceInstanceSummary;
import io.harness.ng.overview.dto.ActiveServiceInstanceSummaryV2;
import io.harness.ng.overview.dto.ActiveServiceInstanceSummaryV3;
import io.harness.ng.overview.dto.ArtifactInstanceDetails;
import io.harness.ng.overview.dto.ChartVersionInstanceDetails;
import io.harness.ng.overview.dto.DashboardWorkloadDeployment;
import io.harness.ng.overview.dto.DashboardWorkloadDeploymentV2;
import io.harness.ng.overview.dto.EnvBuildIdAndInstanceCountInfoList;
import io.harness.ng.overview.dto.EnvIdCountPair;
import io.harness.ng.overview.dto.EnvironmentDeploymentInfo;
import io.harness.ng.overview.dto.EnvironmentGroupInstanceDetails;
import io.harness.ng.overview.dto.ExecutionDeploymentInfo;
import io.harness.ng.overview.dto.HealthDeploymentDashboard;
import io.harness.ng.overview.dto.HealthDeploymentDashboardV2;
import io.harness.ng.overview.dto.InstanceGroupedByEnvironmentList;
import io.harness.ng.overview.dto.InstanceGroupedByServiceList;
import io.harness.ng.overview.dto.InstanceGroupedOnArtifactList;
import io.harness.ng.overview.dto.InstanceGroupedOnChartVersionList;
import io.harness.ng.overview.dto.InstancesByBuildIdList;
import io.harness.ng.overview.dto.OpenTaskDetails;
import io.harness.ng.overview.dto.PipelineExecutionCountInfo;
import io.harness.ng.overview.dto.SequenceToggleDTO;
import io.harness.ng.overview.dto.ServiceDeploymentInfoDTO;
import io.harness.ng.overview.dto.ServiceDeploymentListInfo;
import io.harness.ng.overview.dto.ServiceDeploymentListInfoV2;
import io.harness.ng.overview.dto.ServiceDeploymentMetrics;
import io.harness.ng.overview.dto.ServiceDeploymentsList;
import io.harness.ng.overview.dto.ServiceDetailsInfoDTO;
import io.harness.ng.overview.dto.ServiceDetailsInfoDTOV2;
import io.harness.ng.overview.dto.ServiceHeaderInfo;
import io.harness.ng.overview.dto.TimeValuePairListDTO;
import io.harness.ng.overview.service.CDOverviewDashboardService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Api("dashboard")
@Path("/dashboard")
@NextGenManagerAuth
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Tag(name = "Service Dashboard", description = "This contains APIs related to Service Dashboard")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.BAD_REQUEST_CODE,
    description = NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = FailureDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE,
    description = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = ErrorDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = ErrorDTO.class))
    })
@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_DASHBOARD})
public class CDDashboardOverviewResource {
  private final CDOverviewDashboardService cdOverviewDashboardService;
  private final long HR_IN_MS = 60 * 60 * 1000;
  private final long DAY_IN_MS = 24 * HR_IN_MS;

  private long epochShouldBeOfStartOfDay(long epoch) {
    return epoch - epoch % DAY_IN_MS;
  }
  @GET
  @Path("/deploymentHealth")
  @ApiOperation(value = "Get deployment health", nickname = "getDeploymentHealth")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @Hidden
  public ResponseDTO<HealthDeploymentDashboard> getDeploymentHealth(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting deployment health");

    long numDays = getNumberOfDays(startInterval, endInterval);
    long previousStartInterval = startInterval - getStartTimeOfPreviousInterval(startInterval, numDays);
    cdOverviewDashboardService.validateDashboardRequestDuration(numDays);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getHealthDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval));
  }

  @GET
  @Path("/deploymentHealthV2")
  @ApiOperation(value = "Get deployment health V2", nickname = "getDeploymentHealthV2")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @Hidden
  public ResponseDTO<HealthDeploymentDashboardV2> getDeploymentHealthV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting deployment health");

    long numDays = getNumberOfDays(startInterval, endInterval);
    long previousStartInterval = startInterval - getStartTimeOfPreviousInterval(startInterval, numDays);
    cdOverviewDashboardService.validateDashboardRequestDuration(numDays);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getHealthDeploymentDashboardV2(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval));
  }

  @GET
  @Path("/serviceDeployments")
  @ApiOperation(value = "Get service deployment", nickname = "getServiceDeployments")
  @Hidden
  public ResponseDTO<ServiceDeploymentInfoDTO> getServiceDeployment(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) {
    log.info("Getting service deployments between %s and %s", startTime, endTime);
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeployments(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/serviceDeploymentsInfo")
  @ApiOperation(value = "Get service deployments info", nickname = "getServiceDeploymentsInfo")
  @Hidden
  public ResponseDTO<ServiceDeploymentListInfo> getDeploymentExecutionInfo(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) throws Exception {
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeploymentsInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/serviceDeploymentsInfoV2")
  @ApiOperation(value = "Get service deployments info v2", nickname = "getServiceDeploymentsInfoV2")
  @Hidden
  public ResponseDTO<ServiceDeploymentListInfoV2> getDeploymentExecutionInfoV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) throws Exception {
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeploymentsInfoV2(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/serviceDeploymentMetrics")
  @ApiOperation(value = "Get service deployment metrics", nickname = "getServiceDeploymentMetrics")
  @Hidden
  public ResponseDTO<ServiceDeploymentMetrics> getServiceDeploymentMetrics(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) throws Exception {
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeploymentMetrics(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/serviceDeploymentsList")
  @ApiOperation(value = "Get service deployments list", nickname = "getServiceDeploymentsList")
  @Hidden
  public ResponseDTO<ServiceDeploymentsList> getServiceDeploymentsList(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) throws Exception {
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeploymentsList(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/deploymentExecution")
  @ApiOperation(value = "Get deployment execution", nickname = "getDeploymentExecution")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @Hidden
  public ResponseDTO<ExecutionDeploymentInfo> getDeploymentExecution(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting deployment execution");
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getExecutionDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/getDeployments")
  @ApiOperation(value = "Get deployments", nickname = "getDeployments")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @Hidden
  public ResponseDTO<DashboardExecutionStatusInfo> getDeployments(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @QueryParam("top") @DefaultValue("20") long days) {
    log.info("Getting deployments for active failed and running status");
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentActiveFailedRunningInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days, startInterval, endInterval));
  }

  @GET
  @Path("/getWorkloads")
  @ApiOperation(value = "Get workloads", nickname = "getWorkloads")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @Hidden
  public ResponseDTO<DashboardWorkloadDeployment> getWorkloads(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @QueryParam(NGServiceConstants.ENVIRONMENT_TYPE) EnvironmentType envType) {
    log.info("Getting workloads");
    long numDays = getNumberOfDays(startInterval, endInterval);
    long previousStartInterval = getStartTimeOfPreviousInterval(startInterval, numDays);
    cdOverviewDashboardService.validateDashboardRequestDuration(numDays);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getDashboardWorkloadDeployment(accountIdentifier,
        orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval, envType));
  }

  @GET
  @Path("/getWorkloadsV2")
  @ApiOperation(value = "Get workloads", nickname = "getWorkloadsV2")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @Hidden
  public ResponseDTO<DashboardWorkloadDeploymentV2> getWorkloadsV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @QueryParam(NGServiceConstants.ENVIRONMENT_TYPE) EnvironmentType envType) {
    log.info("Getting workloads");
    long numDays = getNumberOfDays(startInterval, endInterval);
    long previousStartInterval = getStartTimeOfPreviousInterval(startInterval, numDays);
    cdOverviewDashboardService.validateDashboardRequestDuration(numDays);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getDashboardWorkloadDeploymentV2(accountIdentifier,
        orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval, envType));
  }

  @GET
  @Path("/serviceDetails")
  @ApiOperation(value = "Get service details list", nickname = "getServiceDetails")
  @Hidden
  public ResponseDTO<ServiceDetailsInfoDTO> getServiceDeployments(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime,
      @Parameter(description = "Specifies the sorting criteria of the list") @QueryParam("sort") List<String> sort)
      throws Exception {
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDetailsList(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, sort));
  }

  @GET
  @Path("/serviceDetailsV2")
  @ApiOperation(value = "Get service details list v2", nickname = "getServiceDetailsV2")
  @Hidden
  public ResponseDTO<ServiceDetailsInfoDTOV2> getServiceDeploymentsV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime,
      @Parameter(description = "Specifies the sorting criteria of the list") @QueryParam("sort") List<String> sort,
      @Parameter(description = "Specifies the repo name of the entity", hidden = true) @QueryParam(
          "repoName") String repoName) throws Exception {
    cdOverviewDashboardService.validateDashboardRequestDuration(startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDetailsListV2(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, sort, repoName));
  }

  @GET
  @Path("/getServicesGrowthTrend")
  @ApiOperation(value = "Get service growth trend", nickname = "getServicesGrowthTrend")
  @Hidden
  public ResponseDTO<io.harness.ng.overview.dto.TimeValuePairListDTO<Integer>> getServicesGrowthTrend(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.TIME_GROUP_BY_TYPE) TimeGroupType timeGroupType) {
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServicesGrowthTrend(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, timeGroupType));
  }

  @GET
  @Path("/getInstanceCountDetailsByService")
  @ApiOperation(value = "Get active service instance count breakdown by env type",
      nickname = "getActiveServiceInstanceCountBreakdown")
  @Hidden
  public ResponseDTO<InstanceCountDetailsByEnvTypeAndServiceId>
  getActiveServiceInstanceCountBreakdown(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) List<String> serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceInstanceCountBreakdown(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getActiveServiceInstanceSummary")
  @ApiOperation(value = "Get active service instance summary", nickname = "getActiveServiceInstanceSummary")
  @Hidden
  public ResponseDTO<ActiveServiceInstanceSummary> getActiveServiceInstanceSummary(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.TIMESTAMP) long timestampInMs) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceInstanceSummary(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs));
  }

  @GET
  @Path("/getActiveServiceInstanceSummaryV2")
  @ApiOperation(value = "Get active service instance summary v2", nickname = "getActiveServiceInstanceSummaryV2")
  @Hidden
  public ResponseDTO<ActiveServiceInstanceSummaryV2> getActiveServiceInstanceSummaryV2(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.TIMESTAMP) long timestampInMs) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceInstanceSummaryV2(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs));
  }

  @GET
  @Path("/v3/getActiveServiceInstanceSummary")
  @ApiOperation(value = "Get active service instance summary v3", nickname = "getActiveServiceInstanceSummaryV3")
  @Hidden
  public ResponseDTO<ActiveServiceInstanceSummaryV3> getActiveServiceInstanceSummaryV3(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.TIMESTAMP) long timestampInMs) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceInstanceSummaryV3(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs));
  }

  @GET
  @Path("/getEnvBuildInstanceCountByService")
  @ApiOperation(
      value = "Get list of unique environment and build ids with instance count", nickname = "getEnvBuildInstanceCount")
  @Hidden
  public ResponseDTO<EnvBuildIdAndInstanceCountInfoList>
  getEnvBuildInstanceCount(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getEnvBuildInstanceCountByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getActiveServiceInstances")
  @ApiOperation(
      value = "Get list of artifact version, last pipeline execution, environment, infrastructure with instance count",
      nickname = "getActiveServiceInstances")
  @Hidden
  public ResponseDTO<InstanceGroupedByServiceList.InstanceGroupedByService>
  getEnvBuildInstanceCountV2(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceGroupedByArtifactList(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getActiveInstanceGroupedByEnvironment")
  @ApiOperation(value = "Get active instance count for a service grouped on environment, infrastructure, artifact",
      nickname = "getActiveInstanceGroupedByEnvironment")
  @Hidden
  public ResponseDTO<InstanceGroupedByEnvironmentList>
  getActiveInstanceGroupedByEnvironment(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_IDENTIFIER_KEY) String environmentId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) String envGrpId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceGroupedByEnvironmentList(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, environmentId, envGrpId));
  }

  @GET
  @Path("/getActiveInstanceGroupedByArtifact")
  @ApiOperation(value = "Get active instance count for a service grouped on artifact, environment, infrastructure",
      nickname = "getActiveInstanceGroupedByArtifact")
  @Hidden
  public ResponseDTO<InstanceGroupedOnArtifactList>
  getActiveInstanceGroupedByEnvironment(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_IDENTIFIER_KEY) String environmentId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) String envGrpId,
      @QueryParam(NGCommonEntityConstants.ARTIFACT) String displayName,
      @NotNull @QueryParam("filterOnArtifact") boolean filterOnArtifact) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceGroupedOnArtifactList(accountIdentifier,
        orgIdentifier, projectIdentifier, serviceId, environmentId, envGrpId, displayName, filterOnArtifact));
  }

  @GET
  @Path("/getActiveInstanceGroupedByChartVersion")
  @ApiOperation(
      value = "Get active instance count for a service grouped on chart version, artifact, environment, infrastructure",
      nickname = "getActiveInstanceGroupedByChartVersion")
  @Hidden
  public ResponseDTO<InstanceGroupedOnChartVersionList>
  getActiveInstanceGroupedByChartVersion(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_IDENTIFIER_KEY) String environmentId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) String envGrpId,
      @QueryParam(NGCommonEntityConstants.CHART_VERSION) String chartVersion,
      @NotNull @QueryParam(NGCommonEntityConstants.FILTER_ON_CHART_VERSION) boolean filterOnChartVersion) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceGroupedOnChartVersionList(accountIdentifier,
        orgIdentifier, projectIdentifier, serviceId, environmentId, envGrpId, chartVersion, filterOnChartVersion));
  }

  @GET
  @Path("/getInstancesByServiceEnvAndBuilds")
  @ApiOperation(value = "Get list of buildId and instances", nickname = "getActiveInstancesByServiceIdEnvIdAndBuildIds")
  @Hidden
  public ResponseDTO<InstancesByBuildIdList> getActiveInstancesByServiceIdEnvIdAndBuildIds(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.ENVIRONMENT_KEY) String envId,
      @NotNull @QueryParam(NGCommonEntityConstants.BUILDS_KEY) List<String> buildIds,
      @QueryParam(NGCommonEntityConstants.INFRA_IDENTIFIER) String infraIdentifier,
      @QueryParam(NGCommonEntityConstants.CLUSTER_IDENTIFIER) String clusterIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_EXECUTION_ID) String pipelineExecutionId) {
    return ResponseDTO.newResponse(
        cdOverviewDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(accountIdentifier, orgIdentifier,
            projectIdentifier, serviceId, envId, buildIds, infraIdentifier, clusterIdentifier, pipelineExecutionId));
  }

  @GET
  @Path("/getInstancesDetails")
  @ApiOperation(
      value = "Get list of instances grouped by serviceId, buildId, environment, infrastructure and pipeline execution",
      nickname = "getInstancesDetails")
  @Hidden
  public ResponseDTO<InstanceDetailsByBuildId>
  getActiveInstancesDetails(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.ENVIRONMENT_KEY) String envId,
      @QueryParam(NGCommonEntityConstants.INFRA_IDENTIFIER) String infraId,
      @QueryParam(NGCommonEntityConstants.CLUSTER_IDENTIFIER) String clusterId,
      @QueryParam(NGCommonEntityConstants.PIPELINE_EXECUTION_ID) String pipelineExecutionId,
      @QueryParam(NGCommonEntityConstants.BUILD_KEY) String buildId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveInstanceDetails(accountIdentifier, orgIdentifier,
        projectIdentifier, serviceId, envId, infraId, clusterId, pipelineExecutionId, buildId));
  }

  @GET
  @Path("/getActiveServiceInstanceDetailsGroupedByPipelineExecution")
  @ApiOperation(value = "Get list of active instance metadata grouped by pipeline execution for a service",
      nickname = "getActiveServiceInstanceDetailsGroupedByPipelineExecution")
  @Hidden
  public ResponseDTO<InstanceDetailGroupedByPipelineExecutionList>
  getInstanceDetailGroupedByPipelineExecutionList(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.ENVIRONMENT_KEY) String envId,
      @QueryParam(NGCommonEntityConstants.ENVIRONMENT_TYPE_KEY) EnvironmentType environmentType,
      @QueryParam(NGCommonEntityConstants.INFRA_IDENTIFIER) String infraId,
      @QueryParam(NGCommonEntityConstants.CLUSTER_IDENTIFIER) String clusterId,
      @QueryParam(NGCommonEntityConstants.ARTIFACT) String displayName,
      @QueryParam(NGCommonEntityConstants.CHART_VERSION) String chartVersion) {
    return ResponseDTO.newResponse(
        cdOverviewDashboardService.getInstanceDetailGroupedByPipelineExecution(accountIdentifier, orgIdentifier,
            projectIdentifier, serviceId, envId, environmentType, infraId, clusterId, displayName, chartVersion));
  }

  @GET
  @Path("/getInstanceGrowthTrend")
  @ApiOperation(value = "Get instance growth trend", nickname = "getInstanceGrowthTrend")
  @Hidden
  public ResponseDTO<io.harness.ng.overview.dto.TimeValuePairListDTO<Integer>> getInstanceGrowthTrend(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceGrowthTrend(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval, endInterval));
  }

  @GET
  @Path("/getInstanceCountHistory")
  @ApiOperation(value = "Get instance count history", nickname = "getInstanceCountHistory")
  @Hidden
  public ResponseDTO<TimeValuePairListDTO<EnvIdCountPair>> getInstanceCountHistory(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceCountHistory(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval, endInterval));
  }

  @GET
  @Path("/getDeploymentsByServiceId")
  @ApiOperation(value = "Get deployments by serviceId", nickname = "getDeploymentsByServiceId")
  @Hidden
  public ResponseDTO<DeploymentsInfo> getDeploymentsByServiceId(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentsByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval, endInterval));
  }

  @GET
  @Path("/getAllDeploymentsByServiceId")
  @ApiOperation(value = "Get all deployments by serviceId", nickname = "getAllDeploymentsByServiceId")
  @Hidden
  public ResponseDTO<ServiceDeployments> getAllDeploymentsByServiceId(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    cdOverviewDashboardService.validateDashboardRequestDuration(startInterval, endInterval);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getAllDeploymentsByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval, endInterval));
  }

  @GET
  @Path("/getServiceHeaderInfo")
  @ApiOperation(value = "Get service header info", nickname = "getServiceHeaderInfo")
  @Hidden
  public ResponseDTO<ServiceHeaderInfo> getServiceHeaderInfo(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @Parameter(description = "This contains details of Git Entity like Git Branch info",
          hidden = true) @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @Parameter(description = "Specifies whether to load the entity from cache", hidden = true) @HeaderParam(
          "Load-From-Cache") @DefaultValue("false") String loadFromCache,
      @Parameter(description = "Specifies whether to load the entity from fallback branch", hidden = true) @QueryParam(
          "loadFromFallbackBranch") @DefaultValue("false") boolean loadFromFallbackBranch) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceHeaderInfo(accountIdentifier, orgIdentifier,
        projectIdentifier, serviceId, GitXUtils.parseLoadFromCacheHeaderParam(loadFromCache), loadFromFallbackBranch));
  }

  @GET
  @Path("/getEnvArtifactDetailsByServiceId")
  @ApiOperation(value = "Get list of unique environment and Artifact version filter by service_id",
      nickname = "getEnvArtifactDetailsByServiceId")
  @Hidden
  public ResponseDTO<EnvironmentDeploymentInfo>
  getEnvArtifactDetailsByServiceId(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getEnvironmentDeploymentDetailsByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getActiveServiceDeployments")
  @ApiOperation(value = "Get Information about artifacts for a particular service, deployed to different environments",
      nickname = "getActiveServiceDeployments")
  @Hidden
  public ResponseDTO<InstanceGroupedByServiceList.InstanceGroupedByService>
  getActiveServiceDeployments(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceDeploymentsList(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @POST
  @Path("/getEnvironmentInstanceDetails")
  @ApiOperation(
      value = "Get instance count and last artifact deployment detail in each environment for a particular service",
      nickname = "getEnvironmentInstanceDetails")
  @Hidden
  public ResponseDTO<EnvironmentGroupInstanceDetails>
  getEnvironmentInstanceDetails(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @RequestBody(description = "This is the body for the filter properties for listing environments.")
      EnvironmentFilterPropertiesDTO filterProperties) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getEnvironmentInstanceDetails(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, filterProperties, false));
  }

  @GET
  @Path("/getArtifactInstanceDetails")
  @ApiOperation(
      value = "Get last deployment detail in each environment for artifacts having active instances of a service",
      nickname = "getArtifactInstanceDetails")
  @Hidden
  public ResponseDTO<ArtifactInstanceDetails>
  getArtifactInstanceDetails(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getArtifactInstanceDetails(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getChartVersionInstanceDetails")
  @ApiOperation(
      value = "Get last deployment detail in each environment for chart versions having active instances of a service",
      nickname = "getChartVersionInstanceDetails")
  @Hidden
  public ResponseDTO<ChartVersionInstanceDetails>
  getChartVersionInstanceDetails(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getChartVersionInstanceDetails(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getOpenTasks")
  @ApiOperation(value = "Get list of pipelines failed and waiting for approval in 5 days", nickname = "getOpenTasks")
  @Hidden
  public ResponseDTO<OpenTaskDetails> getOpenTasks(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getOpenTasks(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval));
  }

  @GET
  @Path("/getPipelineExecutionCount")
  @ApiOperation(value = "Get pipeline execution count info", nickname = "getPipelineExecutionCount")
  @Operation(operationId = "pipelineExecutionCount",
      summary = "Get pipeline execution count for a service with grouping support on artifact and deployment status",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "Returns pipeline execution count for a service with grouping support on artifact and deployment status")
      })
  public ResponseDTO<PipelineExecutionCountInfo>
  getPipelineExecutionCount(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull
                            @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(description = NGCommonEntityConstants.SERVICE_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @Parameter(description = NGCommonEntityConstants.START_TIME_PARAM_MESSAGE) @QueryParam(
          NGResourceFilterConstants.START_TIME) Long startInterval,
      @Parameter(description = NGCommonEntityConstants.END_TIME_PARAM_MESSAGE) @QueryParam(
          NGResourceFilterConstants.END_TIME) Long endInterval,
      @Parameter(description = NGCommonEntityConstants.ARTIFACT_PATH_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ARTIFACT_PATH) String artifactPath,
      @Parameter(description = NGCommonEntityConstants.ARTIFACT_VERSION_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ARTIFACT_VERSION) String artifactVersion,
      @Parameter(description = NGCommonEntityConstants.ARTIFACT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ARTIFACT) String artifact,
      @Parameter(description = NGCommonEntityConstants.STATUS_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.STATUS) String status) {
    return ResponseDTO.newResponse(
        cdOverviewDashboardService.getPipelineExecutionCountInfo(accountIdentifier, orgIdentifier, projectIdentifier,
            serviceId, startInterval, endInterval, artifactPath, artifactVersion, artifact, status));
  }

  @GET
  @Path("/customSequence")
  @Hidden
  @ApiOperation(value = "Get custom sequence for env and env groups", nickname = "getCustomSequence")
  public ResponseDTO<CustomSequenceDTO> getCustomSequence(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(
        cdOverviewDashboardService.getCustomSequence(accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @POST
  @Path("/customSequence")
  @Hidden
  @ApiOperation(value = "Save custom sequence for env and env groups", nickname = "saveCustomSequence")
  public ResponseDTO<ServiceSequence> saveCustomSequence(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @RequestBody(
          required = true, description = "custom sequence for env and env grps") CustomSequenceDTO customSequenceDTO) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.saveCustomSequence(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, customSequenceDTO));
  }

  @GET
  @Path("/defaultSequence")
  @Hidden
  @ApiOperation(value = "Get default sequence for env and env groups", nickname = "DefaultSequence")
  public ResponseDTO<CustomSequenceDTO> getDefaultSequence(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(
        cdOverviewDashboardService.getDefaultSequence(accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @POST
  @Path("/useCustomSequence")
  @Hidden
  @ApiOperation(value = "Save the status of current sequence of env cards ", nickname = "setCustomSequenceStatus")
  public ResponseDTO<ServiceSequence> useCustomSequence(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam("useCustomSequence") boolean useCustomSequence) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.useCustomSequence(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, useCustomSequence));
  }

  @GET
  @Path("/useCustomSequence")
  @Hidden
  @ApiOperation(value = "get the status of current sequence of env cards ", nickname = "getCustomSequenceStatus")
  public ResponseDTO<SequenceToggleDTO> useCustomSequence(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(
        cdOverviewDashboardService.useCustomSequence(accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }
}
