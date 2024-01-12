/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.mapper;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.rule.OwnerRule.NISHANT;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.rule.Owner;
import io.harness.security.X509Utils;
import io.harness.spec.server.ng.v1.model.CertificateDTO;
import io.harness.spec.server.ng.v1.model.CertificateDetailsDTO;
import io.harness.spec.server.ng.v1.model.CertificateInputSpecType;
import io.harness.spec.server.ng.v1.model.CertificateIssuanceInfo;
import io.harness.spec.server.ng.v1.model.CertificateResponseDTO;
import io.harness.spec.server.ng.v1.model.FileCertificateInputSpecDTO;
import io.harness.spec.server.ng.v1.model.TextCertificateInputSpecDTO;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class NgCertificateMapperTest extends CategoryTest {
  private NgCertificateMapper ngCertificateMapper;

  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";

  private static final String ORG_UNIQUE_ID = UUIDGenerator.generateUuid();
  private static final String PROJECT_UNIQUE_ID = UUIDGenerator.generateUuid();

  @Before
  public void setup() {
    this.ngCertificateMapper = new NgCertificateMapper(new FileUploadLimit());
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToNgCertificate_for_text_input() {
    try (MockedStatic<X509Utils> x509UtilsMockedStatic = Mockito.mockStatic(X509Utils.class)) {
      ScopeInfo scopeInfo = getProjectScopeInfo();
      String certificateText = randomAlphabetic(20);
      CertificateDTO certificateDTO = new CertificateDTO().inputSpec(
          new TextCertificateInputSpecDTO().value(certificateText).type(CertificateInputSpecType.TEXT));
      certificateDTO.identifier(randomAlphabetic(10))
          .tags(Map.of("key1", "tag1"))
          .description(randomAlphabetic(20))
          .name(randomAlphabetic(10))
          .org(ORG_IDENTIFIER)
          .project(PROJECT_IDENTIFIER);
      NgCertificate ngCertificate =
          ngCertificateMapper.toNgCertificate(ACCOUNT_IDENTIFIER, scopeInfo, certificateDTO, null);
      assertThat(ngCertificate)
          .isNotNull()
          .isEqualToComparingFieldByField(NgCertificate.builder()
                                              .identifier(certificateDTO.getIdentifier())
                                              .name(certificateDTO.getName())
                                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                                              .parentUniqueId(PROJECT_UNIQUE_ID)
                                              .tags(convertToList(certificateDTO.getTags()))
                                              .description(certificateDTO.getDescription())
                                              .inputSpecType(CertificateInputSpecType.TEXT)
                                              .certificate(encodeBase64(certificateText))
                                              .build());
    }
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToNgCertificate_for_file_input() {
    try (MockedStatic<X509Utils> x509UtilsMockedStatic = Mockito.mockStatic(X509Utils.class)) {
      ScopeInfo scopeInfo = getOrgScopeInfo();
      String certificateText = randomAlphabetic(20);
      CertificateDTO certificateDTO =
          new CertificateDTO().inputSpec(new FileCertificateInputSpecDTO().type(CertificateInputSpecType.FILE));
      certificateDTO.identifier(randomAlphabetic(10))
          .tags(Map.of("key1", "tag1"))
          .description(randomAlphabetic(20))
          .name(randomAlphabetic(10))
          .org(ORG_IDENTIFIER);
      NgCertificate ngCertificate = ngCertificateMapper.toNgCertificate(
          ACCOUNT_IDENTIFIER, scopeInfo, certificateDTO, new ByteArrayInputStream(certificateText.getBytes()));
      assertThat(ngCertificate)
          .isNotNull()
          .isEqualToComparingFieldByField(NgCertificate.builder()
                                              .identifier(certificateDTO.getIdentifier())
                                              .name(certificateDTO.getName())
                                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                                              .parentUniqueId(ORG_UNIQUE_ID)
                                              .tags(convertToList(certificateDTO.getTags()))
                                              .description(certificateDTO.getDescription())
                                              .inputSpecType(CertificateInputSpecType.FILE)
                                              .certificate(encodeBase64(certificateText))
                                              .build());
    }
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToNgCertificate_for_invalid_certificate() {
    ScopeInfo scopeInfo = getProjectScopeInfo();
    String certificateText = randomAlphabetic(20);
    CertificateDTO certificateDTO = new CertificateDTO().inputSpec(
        new TextCertificateInputSpecDTO().value(certificateText).type(CertificateInputSpecType.TEXT));
    certificateDTO.identifier(randomAlphabetic(10))
        .tags(Map.of("key1", "tag1"))
        .description(randomAlphabetic(20))
        .name(randomAlphabetic(10))
        .org(ORG_IDENTIFIER)
        .project(PROJECT_IDENTIFIER);
    assertThatThrownBy(() -> ngCertificateMapper.toNgCertificate(ACCOUNT_IDENTIFIER, scopeInfo, certificateDTO, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Certificate is not a valid X509Certificate");
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToCertificateDTO_for_text_input() {
    String uniqueId = UUIDGenerator.generateUuid();
    String name = randomAlphabetic(10);
    String certificate = randomAlphabetic(20);
    String identifier = randomAlphabetic(10);
    long createdAt = System.currentTimeMillis() - 1000 * 60 * 10;
    long lastUpdatedAt = System.currentTimeMillis() - 1000 * 60 * 5;
    NgCertificate ngCertificate = getNgCertificate(uniqueId, name, certificate, identifier, PROJECT_UNIQUE_ID,
        CertificateInputSpecType.TEXT, createdAt, lastUpdatedAt);
    ScopeInfo projectScopeInfo = getProjectScopeInfo();
    CertificateDTO certificateDTO = ngCertificateMapper.toCertificateDTO(ngCertificate, projectScopeInfo);
    CertificateDTO expectedCertificateDTO = new CertificateDTO().inputSpec(
        new TextCertificateInputSpecDTO().value(certificate).type(CertificateInputSpecType.TEXT));
    expectedCertificateDTO.certificateValue(certificate)
        .project(PROJECT_IDENTIFIER)
        .org(ORG_IDENTIFIER)
        .identifier(identifier)
        .name(name)
        .description(ngCertificate.getDescription())
        .tags(Map.of("key", "tag"));
    assertThat(certificateDTO).isNotNull().isEqualToComparingFieldByField(expectedCertificateDTO);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToCertificateDTO_for_file_input() {
    String uniqueId = UUIDGenerator.generateUuid();
    String name = randomAlphabetic(10);
    String certificate = randomAlphabetic(20);
    String identifier = randomAlphabetic(10);
    long createdAt = System.currentTimeMillis() - 1000 * 60 * 10;
    long lastUpdatedAt = System.currentTimeMillis() - 1000 * 60 * 5;
    NgCertificate ngCertificate = getNgCertificate(uniqueId, name, certificate, identifier, ACCOUNT_IDENTIFIER,
        CertificateInputSpecType.FILE, createdAt, lastUpdatedAt);
    ScopeInfo accountScopeInfo = getAccountScopeInfo();
    CertificateDTO certificateDTO = ngCertificateMapper.toCertificateDTO(ngCertificate, accountScopeInfo);
    CertificateDTO expectedCertificateDTO =
        new CertificateDTO().inputSpec(new FileCertificateInputSpecDTO().type(CertificateInputSpecType.FILE));
    expectedCertificateDTO.certificateValue(certificate)
        .identifier(identifier)
        .name(name)
        .description(ngCertificate.getDescription())
        .tags(Map.of("key", "tag"));
    assertThat(certificateDTO).isNotNull().isEqualToComparingFieldByField(expectedCertificateDTO);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToCertificateResponseDTO() throws CertificateException {
    String uniqueId = UUIDGenerator.generateUuid();
    String name = randomAlphabetic(10);
    String certificate = getX509Certificate();
    String identifier = randomAlphabetic(10);
    long createdAt = System.currentTimeMillis() - 1000 * 60 * 10;
    long lastUpdatedAt = System.currentTimeMillis() - 1000 * 60 * 5;
    NgCertificate ngCertificate = getNgCertificate(uniqueId, name, certificate, identifier, ACCOUNT_IDENTIFIER,
        CertificateInputSpecType.FILE, createdAt, lastUpdatedAt);
    ScopeInfo accountScopeInfo = getAccountScopeInfo();
    CertificateDTO expectedCertificateDTO =
        new CertificateDTO().inputSpec(new FileCertificateInputSpecDTO().type(CertificateInputSpecType.FILE));
    expectedCertificateDTO.certificateValue(certificate)
        .identifier(identifier)
        .name(name)
        .description(ngCertificate.getDescription())
        .tags(Map.of("key", "tag"));

    CertificateResponseDTO expectedCertificateResponseDTO = new CertificateResponseDTO()
                                                                .certificate(expectedCertificateDTO)
                                                                .certificateDetails(getExpectedCertificateDetailsDTO())
                                                                .created(createdAt)
                                                                .updated(lastUpdatedAt);
    CertificateResponseDTO certificateResponseDTO =
        ngCertificateMapper.toCertificateResponseDTO(ngCertificate, accountScopeInfo);
    assertThat(certificateResponseDTO).isNotNull().isEqualToComparingFieldByField(expectedCertificateResponseDTO);
  }

  private static NgCertificate getNgCertificate(String uniqueId, String name, String certificate, String identifier,
      String parentUniqueId, CertificateInputSpecType specType, long createdAt, long lastUpdatedAt) {
    return NgCertificate.builder()
        .id(UUIDGenerator.generateUuid())
        .uniqueId(uniqueId)
        .parentUniqueId(parentUniqueId)
        .identifier(identifier)
        .inputSpecType(specType)
        .certificate(encodeBase64(certificate))
        .description(randomAlphabetic(10))
        .tags(List.of(NGTag.builder().key("key").value("tag").build()))
        .createdAt(createdAt)
        .lastModifiedDate(lastUpdatedAt)
        .name(name)
        .build();
  }

  private ScopeInfo getAccountScopeInfo() {
    return ScopeInfo.builder()
        .scopeType(ScopeLevel.ACCOUNT)
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .uniqueId(ACCOUNT_IDENTIFIER)
        .build();
  }

  private ScopeInfo getOrgScopeInfo() {
    return ScopeInfo.builder()
        .scopeType(ScopeLevel.ORGANIZATION)
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .orgIdentifier(ORG_IDENTIFIER)
        .uniqueId(ORG_UNIQUE_ID)
        .build();
  }

  private ScopeInfo getProjectScopeInfo() {
    return ScopeInfo.builder()
        .scopeType(ScopeLevel.PROJECT)
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .orgIdentifier(ORG_IDENTIFIER)
        .projectIdentifier(PROJECT_IDENTIFIER)
        .uniqueId(PROJECT_UNIQUE_ID)
        .build();
  }

  private String getX509Certificate() {
    return "-----BEGIN CERTIFICATE-----\n"
        + "MIIDUjCCAjoCCQDjLpTglf4IazANBgkqhkiG9w0BAQsFADAhMQswCQYDVQQGEwJJ\n"
        + "TjESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI0MDEwMzA2MzAwNloXDTI0MDEwNDA2\n"
        + "MzAwNlowgbQxCzAJBgNVBAYTAklOMRgwFgYDVQQIDA9UZXN0IFN0YXRlIE5hbWUx\n"
        + "GzAZBgNVBAcMElRlc3QgTG9jYWxpdHkgTmFtZTEWMBQGA1UECgwNVGVzdCBPcmcg\n"
        + "TmFtZTEbMBkGA1UECwwSVGVzdCBPcmcgVW5pdCBOYW1lMRcwFQYDVQQDDA50ZXN0\n"
        + "Lmhvc3QubmFtZTEgMB4GCSqGSIb3DQEJARYRdGVzdGVtYWlsQGFiYy5wcXIwggEi\n"
        + "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQD1p2+thYxCuh1YZ+DWLd5QUs+c\n"
        + "57atSH4dSfQSDtm0GjxNebCeYSqEoWstbgXYK648Bi8uatvXt+NXih7zjlDvB4k2\n"
        + "JlSB64kS/foKGRpZzIfh+elYUCAZOErm0EnpOvZZo7TR6pBrx+v+pEibj23D6p+f\n"
        + "Gmt22XZ2qQTBvqfMowcLeRqDXSlxbKkf/8amxhw9tnki+7Zhu3fRzvUtTpb+Er0K\n"
        + "CcMYAlRpQIagz/4BCNGH9duDkHWQ+i8LK45kYePQinbQaRVRAOkBLJHeSJiCq0ja\n"
        + "mFJEANs6AGmT4E6VTCvd60k8q8PXAJ6A1epBzZCmSoMrcyhNmdAGRdCLiT19AgMB\n"
        + "AAEwDQYJKoZIhvcNAQELBQADggEBADJ4kIrsh7p+qNO0dfcePOBsjSv2Fst5pgnO\n"
        + "vggYzYJJ5RbpV/7pTwp1oalMUfTndJL5eY6dtIYypTjC3pofExMp46Jymzu3uvkM\n"
        + "xM0K6OJ1tAbGMJLRQWgE+lYb675D2XCDYY93LHSzn6w1v8+Y6tN5KP6tlTu8IBgA\n"
        + "UOw4QFgPeSKfHfgUwn9v66a13tGqcYthvsaL4OIl9IGAIiMOpjbxN2FRv+jVJ86i\n"
        + "aWay/4VOmmPtGnMPaLRMCxOQklemdvLym8eeeMZMA//WaR/CuScp1ePzrUY03RBY\n"
        + "1hyrUjKjrcM4znehZol+V03xW1PP/qMks9NL4arwXbKqAv2qpCk=\n"
        + "-----END CERTIFICATE-----";
  }

  private CertificateDetailsDTO getExpectedCertificateDetailsDTO() {
    CertificateIssuanceInfo issuedToInfo = new CertificateIssuanceInfo()
                                               .commonName("test.host.name")
                                               .organization("Test Org Name")
                                               .organizationalUnit("Test Org Unit Name");
    CertificateIssuanceInfo issuedByInfo = new CertificateIssuanceInfo().commonName("localhost");
    return new CertificateDetailsDTO()
        .issuedTo(issuedToInfo)
        .issuedBy(issuedByInfo)
        .signatureAlgo("SHA256withRSA")
        .validFrom(1704263406000L)
        .validTo(1704349806000L);
  }
}
