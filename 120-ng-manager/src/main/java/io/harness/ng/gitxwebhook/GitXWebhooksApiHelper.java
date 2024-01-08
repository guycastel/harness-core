/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.gitxwebhook;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.gitsync.gitxwebhooks.dtos.CreateGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.CreateGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.DeleteGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.DeleteGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GetGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GetGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookCriteriaDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.mapper.GitXWebhookMapper;
import io.harness.gitsync.gitxwebhooks.service.GitXWebhookService;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.ng.v1.model.CreateGitXWebhookRequest;
import io.harness.spec.server.ng.v1.model.UpdateGitXWebhookRequest;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(CDC)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@NextGenManagerAuth
public class GitXWebhooksApiHelper {
  private final GitXWebhookService gitXWebhookService;

  public CreateGitXWebhookResponseDTO createGitXWebhook(
      String harnessAccount, String org, String project, CreateGitXWebhookRequest body) {
    CreateGitXWebhookRequestDTO createGitXWebhookRequestDTO =
        GitXWebhookMapper.buildCreateGitXWebhookRequestDTO(Scope.of(harnessAccount, org, project), body);
    return gitXWebhookService.createGitXWebhook(createGitXWebhookRequestDTO);
  }

  public Optional<GetGitXWebhookResponseDTO> getGitXWebhook(
      String harnessAccount, String org, String project, String gitXWebhookIdentifier) {
    GetGitXWebhookRequestDTO getGitXWebhookRequestDTO =
        GitXWebhookMapper.buildGetGitXWebhookRequestDTO(Scope.of(harnessAccount, org, project), gitXWebhookIdentifier);
    return gitXWebhookService.getGitXWebhook(getGitXWebhookRequestDTO);
  }

  public UpdateGitXWebhookResponseDTO updateGitXWebhook(
      String harnessAccount, String org, String project, String gitXWebhookIdentifier, UpdateGitXWebhookRequest body) {
    UpdateGitXWebhookRequestDTO updateGitXWebhookRequestDTO = GitXWebhookMapper.buildUpdateGitXWebhookRequestDTO(body);
    return gitXWebhookService.updateGitXWebhook(UpdateGitXWebhookCriteriaDTO.builder()
                                                    .scope(Scope.of(harnessAccount, org, project))
                                                    .webhookIdentifier(gitXWebhookIdentifier)
                                                    .build(),
        updateGitXWebhookRequestDTO);
  }

  public DeleteGitXWebhookResponseDTO deleteGitXWebhook(
      String harnessAccount, String org, String project, String gitXWebhookIdentifier) {
    DeleteGitXWebhookRequestDTO deleteGitXWebhookRequestDTO = GitXWebhookMapper.buildDeleteGitXWebhookRequestDTO(
        Scope.of(harnessAccount, org, project), gitXWebhookIdentifier);
    return gitXWebhookService.deleteGitXWebhook(deleteGitXWebhookRequestDTO);
  }

  public ListGitXWebhookResponseDTO listGitXWebhooks(
      String harnessAccount, String org, String project, String webhookIdentifier) {
    ListGitXWebhookRequestDTO listGitXWebhookRequestDTO =
        GitXWebhookMapper.buildListGitXWebhookRequestDTO(Scope.of(harnessAccount, org, project), webhookIdentifier);
    return gitXWebhookService.listGitXWebhooks(listGitXWebhookRequestDTO);
  }
}
