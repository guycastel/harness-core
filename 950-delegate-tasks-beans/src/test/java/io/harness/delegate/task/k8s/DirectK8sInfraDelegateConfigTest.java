/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.MLUKIC;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.taskcontext.infra.DirectK8sInfraContext;
import io.harness.taskcontext.infra.InfraContext;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class DirectK8sInfraDelegateConfigTest extends CategoryTest {
  private String delegateId = "del_id";
  private String masterUrl = "master_url";
  private String username = "user_name";
  private String oidcIssuerUrl = "issuer_id";
  private String oidcUsername = "oidc_user_name";
  private String oidcClientId = "client_id";
  private String oidcScopes = "scopes";
  private String namespace = "default";

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToInfraContextEmpty() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig = DirectK8sInfraDelegateConfig.builder().build();
    InfraContext result = k8sInfraDelegateConfig.toInfraContext(null);
    assertThat(result).isNotNull();
    assertThat(result.getConnectorInfo()).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToInfraContextManualCredentialsUserPassword() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                                    .config(KubernetesClusterDetailsDTO.builder()
                                                .masterUrl(masterUrl)
                                                .auth(KubernetesAuthDTO.builder()
                                                          .authType(KubernetesAuthType.USER_PASSWORD)
                                                          .credentials(KubernetesUserNamePasswordDTO.builder()
                                                                           .username(username)
                                                                           .passwordRef(SecretRefData.builder().build())
                                                                           .build())
                                                          .build())
                                                .build())
                                    .build())
                    .build())
            .namespace(namespace)
            .build();

    InfraContext result = k8sInfraDelegateConfig.toInfraContext(delegateId);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(DirectK8sInfraContext.class);
    DirectK8sInfraContext directK8sInfraContext = (DirectK8sInfraContext) result;
    assertThat(directK8sInfraContext.getConnectorType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS);
    assertThat(directK8sInfraContext.getManualConfigAuthType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sManualConfigAuthType.USER_PASSWORD);
    assertThat(directK8sInfraContext.getDelegateId()).isNotEmpty();
    assertThat(directK8sInfraContext.getMasterUrl()).isNotEmpty();
    assertThat(directK8sInfraContext.getNamespace()).isNotEmpty();
    assertThat(directK8sInfraContext.getConnectorDetails()).isNotEmpty();
    assertThat(result.getConnectorInfo()).isNotEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToInfraContextManualCredentialsClientKeyCert() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(
                        KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder()
                                        .masterUrl(masterUrl)
                                        .auth(KubernetesAuthDTO.builder()
                                                  .authType(KubernetesAuthType.CLIENT_KEY_CERT)
                                                  .credentials(KubernetesClientKeyCertDTO.builder()
                                                                   .clientCertRef(SecretRefData.builder().build())
                                                                   .clientKeyRef(SecretRefData.builder().build())
                                                                   .build())
                                                  .build())
                                        .build())
                            .build())
                    .build())
            .namespace(namespace)
            .build();

    InfraContext result = k8sInfraDelegateConfig.toInfraContext(delegateId);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(DirectK8sInfraContext.class);
    DirectK8sInfraContext directK8sInfraContext = (DirectK8sInfraContext) result;
    assertThat(directK8sInfraContext.getConnectorType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS);
    assertThat(directK8sInfraContext.getManualConfigAuthType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sManualConfigAuthType.CLIENT_KEY_CERT);
    assertThat(directK8sInfraContext.getDelegateId()).isNotEmpty();
    assertThat(directK8sInfraContext.getMasterUrl()).isNotEmpty();
    assertThat(directK8sInfraContext.getNamespace()).isNotEmpty();
    assertThat(directK8sInfraContext.getConnectorDetails()).isNull();
    assertThat(result.getConnectorInfo()).isNotEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToInfraContextManualCredentialsServiceAccount() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                                    .config(KubernetesClusterDetailsDTO.builder()
                                                .masterUrl(masterUrl)
                                                .auth(KubernetesAuthDTO.builder()
                                                          .authType(KubernetesAuthType.SERVICE_ACCOUNT)
                                                          .credentials(KubernetesServiceAccountDTO.builder()
                                                                           .serviceAccountTokenRef(
                                                                               SecretRefData.builder().build())
                                                                           .build())
                                                          .build())
                                                .build())
                                    .build())
                    .build())
            .namespace(namespace)
            .build();

    InfraContext result = k8sInfraDelegateConfig.toInfraContext(delegateId);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(DirectK8sInfraContext.class);
    DirectK8sInfraContext directK8sInfraContext = (DirectK8sInfraContext) result;
    assertThat(directK8sInfraContext.getConnectorType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS);
    assertThat(directK8sInfraContext.getManualConfigAuthType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sManualConfigAuthType.SERVICE_ACCOUNT);
    assertThat(directK8sInfraContext.getDelegateId()).isNotEmpty();
    assertThat(directK8sInfraContext.getMasterUrl()).isNotEmpty();
    assertThat(directK8sInfraContext.getNamespace()).isNotEmpty();
    assertThat(directK8sInfraContext.getConnectorDetails()).isNull();
    assertThat(result.getConnectorInfo()).isNotEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToInfraContextManualCredentialsOpenIdConnect() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(
                        KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder()
                                        .masterUrl(masterUrl)
                                        .auth(KubernetesAuthDTO.builder()
                                                  .authType(KubernetesAuthType.OPEN_ID_CONNECT)
                                                  .credentials(KubernetesOpenIdConnectDTO.builder()
                                                                   .oidcIssuerUrl(oidcIssuerUrl)
                                                                   .oidcUsername(oidcUsername)
                                                                   .oidcClientIdRef(
                                                                       SecretRefData.builder()
                                                                           .decryptedValue(oidcClientId.toCharArray())
                                                                           .build())
                                                                   .oidcPasswordRef(SecretRefData.builder().build())
                                                                   .oidcScopes(oidcScopes)
                                                                   .build())
                                                  .build())
                                        .build())
                            .build())
                    .build())
            .namespace(namespace)
            .build();

    InfraContext result = k8sInfraDelegateConfig.toInfraContext(delegateId);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(DirectK8sInfraContext.class);
    DirectK8sInfraContext directK8sInfraContext = (DirectK8sInfraContext) result;
    assertThat(directK8sInfraContext.getConnectorType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS);
    assertThat(directK8sInfraContext.getManualConfigAuthType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sManualConfigAuthType.OPEN_ID_CONNECT);
    assertThat(directK8sInfraContext.getDelegateId()).isNotEmpty();
    assertThat(directK8sInfraContext.getMasterUrl()).isNotEmpty();
    assertThat(directK8sInfraContext.getNamespace()).isNotEmpty();
    assertThat(directK8sInfraContext.getConnectorDetails()).isNotEmpty();
    assertThat(result.getConnectorInfo()).isNotEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToInfraContextInheritFromDelegate() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                    .config(KubernetesDelegateDetailsDTO.builder().build())
                                    .build())
                    .build())
            .namespace(namespace)
            .build();

    InfraContext result = k8sInfraDelegateConfig.toInfraContext(delegateId);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(DirectK8sInfraContext.class);
    DirectK8sInfraContext directK8sInfraContext = (DirectK8sInfraContext) result;
    assertThat(directK8sInfraContext.getConnectorType())
        .isEqualTo(DirectK8sInfraContext.DirectК8sConnectorType.INHERIT_FROM_DELEGATE);
    assertThat(directK8sInfraContext.getManualConfigAuthType()).isNull();
    assertThat(directK8sInfraContext.getDelegateId()).isNotEmpty();
    assertThat(directK8sInfraContext.getMasterUrl()).isNull();
    assertThat(directK8sInfraContext.getNamespace()).isNotEmpty();
    assertThat(directK8sInfraContext.getConnectorDetails()).isNull();
    assertThat(result.getConnectorInfo()).isNotEmpty();
  }
}
