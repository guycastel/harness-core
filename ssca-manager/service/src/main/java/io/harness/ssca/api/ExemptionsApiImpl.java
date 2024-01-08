/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import static io.harness.ssca.utils.Constants.SSCA_ENFORCEMENT_EXEMPTION;
import static io.harness.ssca.utils.Constants.SSCA_ENFORCEMENT_EXEMPTION_DELETE_PERMISSION;
import static io.harness.ssca.utils.Constants.SSCA_ENFORCEMENT_EXEMPTION_EDIT_PERMISSION;
import static io.harness.ssca.utils.Constants.SSCA_ENFORCEMENT_EXEMPTION_REVIEW_PERMISSION;
import static io.harness.ssca.utils.Constants.SSCA_ENFORCEMENT_EXEMPTION_VIEW_PERMISSION;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.ssca.v1.ExemptionsApi;
import io.harness.spec.server.ssca.v1.model.ExemptionInitiatorDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionRequestDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionReviewRequestDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionStatusDTO;
import io.harness.ssca.services.exemption.ExemptionService;
import io.harness.ssca.utils.PageResponseUtils;

import com.google.inject.Inject;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.springframework.data.domain.PageRequest;

@OwnedBy(HarnessTeam.SSCA)
@NextGenManagerAuth
public class ExemptionsApiImpl implements ExemptionsApi {
  @Inject ExemptionService exemptionService;

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_EDIT_PERMISSION)
  public Response
  createExemptionForArtifact(@ProjectIdentifier String project, @OrgIdentifier String org, String artifact,
      @Valid ExemptionRequestDTO body, @AccountIdentifier String harnessAccount) {
    populateInitiationDetails(project, artifact, body);
    return Response.status(Status.CREATED)
        .entity(exemptionService.createExemption(harnessAccount, project, org, artifact, body))
        .build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_EDIT_PERMISSION)
  public Response
  createExemptionForProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @Valid ExemptionRequestDTO body, @AccountIdentifier String harnessAccount) {
    populateInitiationDetails(project, body);
    return Response.status(Status.CREATED)
        .entity(exemptionService.createExemption(harnessAccount, project, org, null, body))
        .build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_DELETE_PERMISSION)
  public Response
  deleteExemptionForArtifact(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, String artifact, @AccountIdentifier String harnessAccount) {
    exemptionService.deleteExemption(harnessAccount, org, project, artifact, exemption);
    return Response.ok().build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_DELETE_PERMISSION)
  public Response
  deleteExemptionForProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, @AccountIdentifier String harnessAccount) {
    exemptionService.deleteExemption(harnessAccount, org, project, null, exemption);
    return Response.ok().build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_VIEW_PERMISSION)
  public Response
  getExemptionForArtifact(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, String artifact, @AccountIdentifier String harnessAccount) {
    return Response.ok(exemptionService.getExemption(harnessAccount, org, project, artifact, exemption)).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_VIEW_PERMISSION)
  public Response
  getExemptionForProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, @AccountIdentifier String harnessAccount) {
    return Response.ok(exemptionService.getExemption(harnessAccount, org, project, null, exemption)).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_VIEW_PERMISSION)
  public Response
  listExemptionsForProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @AccountIdentifier String harnessAccount, @Min(1L) @Max(1000L) Integer limit, @Min(0L) Integer page,
      List<ExemptionStatusDTO> status, String artifactId, String searchTerm) {
    return PageResponseUtils.getPagedResponse(exemptionService.getExemptions(
        harnessAccount, org, project, artifactId, status, searchTerm, PageRequest.of(page, limit)));
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_REVIEW_PERMISSION)
  public Response
  reviewExemptionForArtifact(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, String artifact, @Valid ExemptionReviewRequestDTO body,
      @AccountIdentifier String harnessAccount) {
    return Response.ok(exemptionService.reviewExemption(harnessAccount, project, org, artifact, exemption, body))
        .build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_REVIEW_PERMISSION)
  public Response
  reviewExemptionForProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, @Valid ExemptionReviewRequestDTO body,
      @AccountIdentifier String harnessAccount) {
    return Response.ok(exemptionService.reviewExemption(harnessAccount, project, org, null, exemption, body)).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_EDIT_PERMISSION)
  public Response
  updateExemptionForArtifact(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, String artifact, @Valid ExemptionRequestDTO body,
      @AccountIdentifier String harnessAccount) {
    populateInitiationDetails(project, artifact, body);
    return Response.ok(exemptionService.updateExemption(harnessAccount, project, org, artifact, exemption, body))
        .build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = SSCA_ENFORCEMENT_EXEMPTION, permission = SSCA_ENFORCEMENT_EXEMPTION_EDIT_PERMISSION)
  public Response
  updateExemptionForProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String exemption, @Valid ExemptionRequestDTO body, @AccountIdentifier String harnessAccount) {
    populateInitiationDetails(project, body);
    return Response.ok(exemptionService.updateExemption(harnessAccount, project, org, null, exemption, body)).build();
  }

  private static void populateInitiationDetails(String project, ExemptionRequestDTO body) {
    populateInitiationDetails(project, null, body);
  }

  private static void populateInitiationDetails(String project, String artifact, ExemptionRequestDTO body) {
    ExemptionInitiatorDTO exemptionInitiatorDTO = body.getExemptionInitiator();
    if (exemptionInitiatorDTO == null) {
      exemptionInitiatorDTO = new ExemptionInitiatorDTO();
    }
    exemptionInitiatorDTO.setArtifactId(artifact);
    exemptionInitiatorDTO.setProjectIdentifier(project);
    body.setExemptionInitiator(exemptionInitiatorDTO);
  }
}