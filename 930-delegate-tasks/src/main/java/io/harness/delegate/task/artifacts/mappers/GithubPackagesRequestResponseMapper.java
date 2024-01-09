/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.mappers;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.artifacts.githubpackages.beans.GithubPackagesInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.githubpackages.GithubPackagesArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.githubpackages.GithubPackagesArtifactDelegateResponse;
import io.harness.encryption.FieldWithPlainTextOrSecretValueHelper;
import io.harness.exception.InvalidRequestException;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ARTIFACTS})
@UtilityClass
public class GithubPackagesRequestResponseMapper {
  public GithubPackagesInternalConfig toGithubPackagesInternalConfig(GithubPackagesArtifactDelegateRequest request) {
    return toGithubPackagesInternalConfig(request.getGithubConnectorDTO());
  }

  public GithubPackagesInternalConfig toGithubPackagesInternalConfig(GithubConnectorDTO githubConnectorDTO) {
    String password = "";
    String username = "";
    String token = "";

    if (githubConnectorDTO.getAuthentication() != null
        && githubConnectorDTO.getAuthentication().getCredentials() != null) {
      if (githubConnectorDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
        GithubHttpCredentialsDTO httpDTO =
            (GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials();

        switch (httpDTO.getType()) {
          case USERNAME_AND_PASSWORD:
            GithubUsernamePasswordDTO githubUsernamePasswordDTO =
                (GithubUsernamePasswordDTO) httpDTO.getHttpCredentialsSpec();
            username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
                githubUsernamePasswordDTO.getUsername(), githubUsernamePasswordDTO.getUsernameRef());
            break;
          case USERNAME_AND_TOKEN:
            GithubUsernameTokenDTO githubUsernameTokenDTO = (GithubUsernameTokenDTO) httpDTO.getHttpCredentialsSpec();
            username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
                githubUsernameTokenDTO.getUsername(), githubUsernameTokenDTO.getUsernameRef());
            break;
          case GITHUB_APP:
            username = GithubAppDTO.username;
            break;
          default:
        }
      }
    }

    GithubApiAccessDTO githubApiAccessDTO = githubConnectorDTO.getApiAccess();

    if (githubApiAccessDTO == null) {
      throw new InvalidRequestException("Please enable the API Access for the Github Connector");
    }

    GithubApiAccessType githubApiAccessType = githubApiAccessDTO.getType();

    if (githubApiAccessType == GithubApiAccessType.TOKEN) {
      GithubTokenSpecDTO githubTokenSpecDTO = (GithubTokenSpecDTO) githubApiAccessDTO.getSpec();

      if (githubTokenSpecDTO.getTokenRef() != null) {
        token = EmptyPredicate.isNotEmpty(githubTokenSpecDTO.getTokenRef().getDecryptedValue())
            ? new String(githubTokenSpecDTO.getTokenRef().getDecryptedValue())
            : null;

      } else {
        throw new InvalidRequestException("The token reference for the Github Connector is null");
      }

    } else {
      throw new InvalidRequestException("Please select the API Access auth type to Token");
    }

    return GithubPackagesInternalConfig.builder()
        .githubPackagesUrl(githubConnectorDTO.getUrl())
        .authMechanism(((GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials())
                           .getType()
                           .getDisplayName())
        .username(username)
        .password(password)
        .token(token)
        .build();
  }

  public GithubPackagesArtifactDelegateResponse toGithubPackagesResponse(
      BuildDetails buildDetails, GithubPackagesArtifactDelegateRequest request) {
    return GithubPackagesArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetails))
        .packageName(request.getPackageName())
        .version(buildDetails.getNumber())
        .versionRegex(request.getVersionRegex())
        .sourceType(ArtifactSourceType.GITHUB_PACKAGES)
        .packageUrl(buildDetails.getArtifactPath())
        .label(buildDetails.getLabels())
        .build();
  }

  public List<GithubPackagesArtifactDelegateResponse> toGithubPackagesResponse(
      List<Map<String, String>> map, GithubPackagesArtifactDelegateRequest request) {
    List<GithubPackagesArtifactDelegateResponse> response = new ArrayList<>();

    for (Map<String, String> resMap : map) {
      GithubPackagesArtifactDelegateResponse githubPackagesArtifactDelegateResponse =
          GithubPackagesArtifactDelegateResponse.builder()
              .packageId(resMap.get("packageId"))
              .packageName(resMap.get("packageName"))
              .packageType(resMap.get("packageType"))
              .packageVisibility(resMap.get("visibility"))
              .packageUrl(resMap.get("packageUrl"))
              .sourceType(ArtifactSourceType.GITHUB_PACKAGES)
              .build();

      response.add(githubPackagesArtifactDelegateResponse);
    }

    return response;
  }
}
