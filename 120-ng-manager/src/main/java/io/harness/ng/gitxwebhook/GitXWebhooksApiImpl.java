/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.gitxwebhook;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.gitsync.gitxwebhooks.dtos.CreateGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.DeleteGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GetGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventsListRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventsListResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.mapper.GitXWebhookMapper;
import io.harness.gitsync.gitxwebhooks.service.GitXWebhookEventService;
import io.harness.gitsync.gitxwebhooks.service.GitXWebhookService;
import io.harness.gitx.GitXWebhhookRbacPermissionsConstants;
import io.harness.spec.server.ng.v1.GitXWebhooksApi;
import io.harness.spec.server.ng.v1.model.CreateGitXWebhookRequest;
import io.harness.spec.server.ng.v1.model.CreateGitXWebhookResponse;
import io.harness.spec.server.ng.v1.model.DeleteGitXWebhookResponse;
import io.harness.spec.server.ng.v1.model.GitXWebhookEventResponse;
import io.harness.spec.server.ng.v1.model.GitXWebhookResponse;
import io.harness.spec.server.ng.v1.model.UpdateGitXWebhookRequest;
import io.harness.spec.server.ng.v1.model.UpdateGitXWebhookResponse;
import io.harness.utils.ApiUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class GitXWebhooksApiImpl implements GitXWebhooksApi {
  GitXWebhookService gitXWebhookService;
  GitXWebhookEventService gitXWebhookEventService;
  GitXWebhooksApiHelper gitXWebhooksApiHelper;
  public static final int HTTP_201 = 201;
  public static final int HTTP_404 = 404;
  public static final int HTTP_204 = 204;

  @Override
  @NGAccessControlCheck(
      resourceType = "GITX_WEBHOOKS", permission = GitXWebhhookRbacPermissionsConstants.GitXWebhhook_CREATE_AND_EDIT)
  public Response
  createGitxWebhook(@Valid CreateGitXWebhookRequest body, @AccountIdentifier String harnessAccount) {
    CreateGitXWebhookResponseDTO createGitXWebhookResponseDTO =
        gitXWebhooksApiHelper.createGitXWebhook(harnessAccount, null, null, body);
    CreateGitXWebhookResponse responseBody =
        GitXWebhookMapper.buildCreateGitXWebhookResponse(createGitXWebhookResponseDTO);
    return Response.status(HTTP_201).entity(responseBody).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = "GITX_WEBHOOKS", permission = GitXWebhhookRbacPermissionsConstants.GitXWebhhook_VIEW)
  public Response
  getGitxWebhook(String gitXWebhookIdentifier, @AccountIdentifier String harnessAccount) {
    Optional<GetGitXWebhookResponseDTO> optionalGetGitXWebhookResponseDTO =
        gitXWebhooksApiHelper.getGitXWebhook(harnessAccount, null, null, gitXWebhookIdentifier);
    if (optionalGetGitXWebhookResponseDTO.isEmpty()) {
      return Response.status(HTTP_404).build();
    }
    GitXWebhookResponse responseBody =
        GitXWebhookMapper.buildGetGitXWebhookResponseDTO(optionalGetGitXWebhookResponseDTO.get());
    return Response.ok().entity(responseBody).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = "GITX_WEBHOOKS", permission = GitXWebhhookRbacPermissionsConstants.GitXWebhhook_CREATE_AND_EDIT)
  public Response
  updateGitxWebhook(
      String gitXWebhookIdentifier, @Valid UpdateGitXWebhookRequest body, @AccountIdentifier String harnessAccount) {
    UpdateGitXWebhookResponseDTO updateGitXWebhookResponseDTO =
        gitXWebhooksApiHelper.updateGitXWebhook(harnessAccount, null, null, gitXWebhookIdentifier, body);
    UpdateGitXWebhookResponse responseBody =
        GitXWebhookMapper.buildUpdateGitXWebhookResponse(updateGitXWebhookResponseDTO);
    return Response.ok().entity(responseBody).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = "GITX_WEBHOOKS", permission = GitXWebhhookRbacPermissionsConstants.GitXWebhhook_DELETE)
  public Response
  deleteGitxWebhook(String gitXWebhookIdentifier, @AccountIdentifier String harnessAccount) {
    DeleteGitXWebhookResponseDTO deleteGitXWebhookResponse =
        gitXWebhooksApiHelper.deleteGitXWebhook(harnessAccount, null, null, gitXWebhookIdentifier);
    DeleteGitXWebhookResponse responseBody =
        GitXWebhookMapper.buildDeleteGitXWebhookResponse(deleteGitXWebhookResponse);
    return Response.status(HTTP_204).entity(responseBody).build();
  }

  @Override
  @NGAccessControlCheck(
      resourceType = "GITX_WEBHOOKS", permission = GitXWebhhookRbacPermissionsConstants.GitXWebhhook_VIEW)
  public Response
  listGitxWebhooks(
      @AccountIdentifier String harnessAccount, Integer page, @Max(1000L) Integer limit, String webhookIdentifier) {
    ListGitXWebhookResponseDTO listGitXWebhookResponseDTO =
        gitXWebhooksApiHelper.listGitXWebhooks(harnessAccount, null, null, webhookIdentifier);
    Page<GitXWebhookResponse> gitXWebhooks =
        GitXWebhookMapper.buildListGitXWebhookResponse(listGitXWebhookResponseDTO, page, limit);

    ResponseBuilder responseBuilder = Response.ok();
    ResponseBuilder responseBuilderWithLinks =
        ApiUtils.addLinksHeader(responseBuilder, gitXWebhooks.getTotalElements(), page, limit);
    return responseBuilderWithLinks
        .entity(gitXWebhooks.getContent()
                    .stream()
                    .map(GitXWebhookMapper::buildGetGitXWebhookResponseDTO)
                    .collect(Collectors.toList()))
        .build();
  }

  @Override
  public Response listGitxWebhookEvents(String harnessAccount, Integer page, @Max(1000L) Integer limit,
      String webhookIdentifier, Long eventStartTime, Long eventEndTime, String repoName, String filePath,
      String eventIdentifier, List<String> eventStatus) {
    GitXEventsListRequestDTO gitXEventsListRequestDTO =
        GitXWebhookMapper.buildEventsListGitXWebhookRequestDTO(Scope.of(harnessAccount), webhookIdentifier,
            eventStartTime, eventEndTime, repoName, filePath, eventIdentifier, eventStatus);
    GitXEventsListResponseDTO gitXEventsListResponseDTO = gitXWebhookEventService.listEvents(gitXEventsListRequestDTO);

    Page<GitXWebhookEventResponse> gitXWebhookEvents =
        GitXWebhookMapper.buildListGitXWebhookEventResponse(gitXEventsListResponseDTO, page, limit);
    ResponseBuilder responseBuilder = Response.ok();
    ResponseBuilder responseBuilderWithLinks =
        ApiUtils.addLinksHeader(responseBuilder, gitXWebhookEvents.getTotalElements(), page, limit);
    return responseBuilderWithLinks
        .entity(gitXWebhookEvents.getContent()
                    .stream()
                    .map(GitXWebhookMapper::buildGitXWebhookEventResponse)
                    .collect(Collectors.toList()))
        .build();
  }
}
