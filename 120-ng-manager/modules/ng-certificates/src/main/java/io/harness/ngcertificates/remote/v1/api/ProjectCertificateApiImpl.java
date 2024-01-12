/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.remote.v1.api;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.services.ScopeInfoService;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.ngcertificates.mapper.NgCertificateMapper;
import io.harness.ngcertificates.services.NgCertificateService;
import io.harness.spec.server.ng.v1.ProjectCertificateApi;
import io.harness.spec.server.ng.v1.model.CertificateDTO;

import com.google.inject.Inject;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ProjectCertificateApiImpl implements ProjectCertificateApi {
  private final NgCertificateService ngCertificateService;
  private final NgCertificateMapper ngCertificateMapper;
  private final ScopeInfoService scopeInfoService;
  @SneakyThrows
  @Override
  public Response createProjectScopedCertificates(
      String org, String project, @Valid CertificateDTO certificateDTO, String harnessAccount) {
    if (!Objects.equals(org, certificateDTO.getOrg()) || !Objects.equals(project, certificateDTO.getProject())) {
      throw new InvalidRequestException(
          "Invalid request, org and project scope in payload and params do not match.", USER);
    }
    Optional<ScopeInfo> scopeInfo = scopeInfoService.getScopeInfo(harnessAccount, org, project);
    NgCertificate ngCertificate = ngCertificateService.create(scopeInfo.orElseThrow(), certificateDTO, null);
    return Response.status(Response.Status.CREATED)
        .entity(ngCertificateMapper.toCertificateResponseDTO(ngCertificate, scopeInfo.orElseThrow()))
        .build();
  }

  @SneakyThrows
  @Override
  public Response createProjectScopedCertificates(
      String org, String project, CertificateDTO certificateDTO, InputStream fileInputStream, String harnessAccount) {
    if (!Objects.equals(org, certificateDTO.getOrg()) || !Objects.equals(project, certificateDTO.getProject())) {
      throw new InvalidRequestException(
          "Invalid request, org and project scope in payload and params do not match.", USER);
    }
    Optional<ScopeInfo> scopeInfo = scopeInfoService.getScopeInfo(harnessAccount, org, project);
    NgCertificate ngCertificate = ngCertificateService.create(scopeInfo.orElseThrow(), certificateDTO, fileInputStream);
    return Response.status(Response.Status.CREATED)
        .entity(ngCertificateMapper.toCertificateResponseDTO(ngCertificate, scopeInfo.orElseThrow()))
        .build();
  }

  @Override
  public Response getProjectScopedCertificates(String org, String project) {
    return null;
  }
}
