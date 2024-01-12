/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.remote.v1.api;

import static io.harness.rule.OwnerRule.NISHANT;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.services.ScopeInfoService;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.ngcertificates.mapper.NgCertificateMapper;
import io.harness.ngcertificates.services.NgCertificateService;
import io.harness.rule.Owner;
import io.harness.spec.server.ng.v1.model.CertificateDTO;
import io.harness.spec.server.ng.v1.model.CertificateInputSpecType;
import io.harness.spec.server.ng.v1.model.CertificateResponseDTO;
import io.harness.spec.server.ng.v1.model.FileCertificateInputSpecDTO;
import io.harness.spec.server.ng.v1.model.TextCertificateInputSpecDTO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProjectCertificateApiImplTest extends CategoryTest {
  @Mock private NgCertificateService ngCertificateService;
  @Mock private NgCertificateMapper ngCertificateMapper;
  @Mock private ScopeInfoService scopeInfoService;
  private ProjectCertificateApiImpl projectCertificateApi;

  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";

  private static final String ORG_UNIQUE_ID = UUIDGenerator.generateUuid();
  private static final String PROJECT_UNIQUE_ID = UUIDGenerator.generateUuid();

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.projectCertificateApi =
        new ProjectCertificateApiImpl(ngCertificateService, ngCertificateMapper, scopeInfoService);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testcreateProjectScopedCertificates_for_text_input() throws CertificateException {
    String certificateText = randomAlphabetic(20);
    NgCertificate ngCertificate = NgCertificate.builder().build();
    CertificateDTO certificateDTO = new CertificateDTO().inputSpec(
        new TextCertificateInputSpecDTO().value(certificateText).type(CertificateInputSpecType.TEXT));
    certificateDTO.identifier(randomAlphabetic(10))
        .org(ORG_IDENTIFIER)
        .project(PROJECT_IDENTIFIER)
        .tags(Map.of("key1", "tag1"))
        .description(randomAlphabetic(20))
        .name(randomAlphabetic(10));
    ScopeInfo orgScopeInfo = getProjectScopeInfo();
    when(scopeInfoService.getScopeInfo(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER))
        .thenReturn(Optional.of(orgScopeInfo));
    when(ngCertificateService.create(orgScopeInfo, certificateDTO, null)).thenReturn(ngCertificate);
    when(ngCertificateMapper.toCertificateResponseDTO(ngCertificate, orgScopeInfo))
        .thenReturn(new CertificateResponseDTO());
    Response response = projectCertificateApi.createProjectScopedCertificates(
        ORG_IDENTIFIER, PROJECT_IDENTIFIER, certificateDTO, ACCOUNT_IDENTIFIER);
    assertThat(response).isNotNull();
    assertThat(response.getEntity()).isNotNull().isInstanceOf(CertificateResponseDTO.class);
    verify(scopeInfoService, times(1)).getScopeInfo(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    verify(ngCertificateService, times(1)).create(orgScopeInfo, certificateDTO, null);
    verify(ngCertificateMapper, times(1)).toCertificateResponseDTO(ngCertificate, orgScopeInfo);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateAccountScopedCertificates_for_wrong_scope_info() {
    String certificateText = randomAlphabetic(20);
    CertificateDTO certificateDTO = new CertificateDTO().inputSpec(
        new TextCertificateInputSpecDTO().value(certificateText).type(CertificateInputSpecType.TEXT));
    certificateDTO.identifier(randomAlphabetic(10))
        .org(randomAlphabetic(10))
        .project(randomAlphabetic(10))
        .tags(Map.of("key1", "tag1"))
        .description(randomAlphabetic(20))
        .name(randomAlphabetic(10));
    assertThatThrownBy(()
                           -> projectCertificateApi.createProjectScopedCertificates(
                               ORG_IDENTIFIER, PROJECT_IDENTIFIER, certificateDTO, ACCOUNT_IDENTIFIER))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid request, org and project scope in payload and params do not match.");
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateAccountScopedCertificates_for_file_input() throws CertificateException {
    String certificateText = randomAlphabetic(20);
    InputStream inputStream = new ByteArrayInputStream(certificateText.getBytes());
    NgCertificate ngCertificate = NgCertificate.builder().build();
    CertificateDTO certificateDTO =
        new CertificateDTO().inputSpec(new FileCertificateInputSpecDTO().type(CertificateInputSpecType.FILE));
    certificateDTO.identifier(randomAlphabetic(10))
        .org(ORG_IDENTIFIER)
        .project(PROJECT_IDENTIFIER)
        .tags(Map.of("key1", "tag1"))
        .description(randomAlphabetic(20))
        .name(randomAlphabetic(10));
    ScopeInfo orgScopeInfo = getProjectScopeInfo();
    when(scopeInfoService.getScopeInfo(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER))
        .thenReturn(Optional.of(orgScopeInfo));
    when(ngCertificateService.create(orgScopeInfo, certificateDTO, inputStream)).thenReturn(ngCertificate);
    when(ngCertificateMapper.toCertificateResponseDTO(ngCertificate, orgScopeInfo))
        .thenReturn(new CertificateResponseDTO());
    Response response = projectCertificateApi.createProjectScopedCertificates(
        ORG_IDENTIFIER, PROJECT_IDENTIFIER, certificateDTO, inputStream, ACCOUNT_IDENTIFIER);
    assertThat(response).isNotNull();
    assertThat(response.getEntity()).isNotNull().isInstanceOf(CertificateResponseDTO.class);
    verify(scopeInfoService, times(1)).getScopeInfo(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    verify(ngCertificateService, times(1)).create(orgScopeInfo, certificateDTO, inputStream);
    verify(ngCertificateMapper, times(1)).toCertificateResponseDTO(ngCertificate, orgScopeInfo);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateAccountScopedCertificates_for_file_with_wrong_scope_info() {
    String certificateText = randomAlphabetic(20);
    CertificateDTO certificateDTO =
        new CertificateDTO().inputSpec(new FileCertificateInputSpecDTO().type(CertificateInputSpecType.TEXT));
    certificateDTO.identifier(randomAlphabetic(10))
        .project(randomAlphabetic(10))
        .org(randomAlphabetic(10))
        .tags(Map.of("key1", "tag1"))
        .description(randomAlphabetic(20))
        .name(randomAlphabetic(10));
    assertThatThrownBy(
        ()
            -> projectCertificateApi.createProjectScopedCertificates(ORG_IDENTIFIER, PROJECT_IDENTIFIER, certificateDTO,
                new ByteArrayInputStream(certificateText.getBytes()), ACCOUNT_IDENTIFIER))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid request, org and project scope in payload and params do not match.");
  }

  private static ScopeInfo getProjectScopeInfo() {
    return ScopeInfo.builder()
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .orgIdentifier(ORG_IDENTIFIER)
        .projectIdentifier(PROJECT_IDENTIFIER)
        .scopeType(ScopeLevel.ACCOUNT)
        .uniqueId(PROJECT_UNIQUE_ID)
        .build();
  }
}
