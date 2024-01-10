/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.taskcontext.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.MLUKIC;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class DirectK8sInfraContextTest extends CategoryTest {
  private String username = "user-name";
  private String namespace = "default";
  private String delegateId = "delegate-id";
  private String masterUrl = "https://masterurl.com:1234/";
  private String oidcIssuerUrl = "issuer_id";
  private String oidcUsername = "oidc_user_name";
  private String oidcClientId = "client_id";
  private String oidcScopes = "scopes";

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToConnectorInfoEmpty() {
    DirectK8sInfraContext directK8sInfraContext = DirectK8sInfraContext.builder().build();
    Optional<String> result = directK8sInfraContext.getConnectorInfo();
    assertThat(result).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToConnectorInfoManualCredentialsUserPassword() {
    DirectK8sInfraContext directK8sInfraContext =
        DirectK8sInfraContext.builder()
            .namespace(namespace)
            .delegateId(delegateId)
            .connectorType(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS)
            .manualConfigAuthType(DirectK8sInfraContext.DirectК8sManualConfigAuthType.USER_PASSWORD)
            .masterUrl(masterUrl)
            .connectorDetails(format("Username: %s", username))
            .build();

    Optional<String> result = directK8sInfraContext.getConnectorInfo();
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo(
            "Kubernetes connector configured with masterUrl [https://masterurl.com:1234/] and credential type [UsernamePassword] and details [Username: user-name]");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToConnectorInfoManualCredentialsClientKeyCert() {
    DirectK8sInfraContext directK8sInfraContext =
        DirectK8sInfraContext.builder()
            .namespace(namespace)
            .delegateId(delegateId)
            .connectorType(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS)
            .manualConfigAuthType(DirectK8sInfraContext.DirectК8sManualConfigAuthType.CLIENT_KEY_CERT)
            .masterUrl(masterUrl)
            .build();

    Optional<String> result = directK8sInfraContext.getConnectorInfo();
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo(
            "Kubernetes connector configured with masterUrl [https://masterurl.com:1234/] and credential type [ClientKeyCert]");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToConnectorInfoManualCredentialsServiceAccount() {
    DirectK8sInfraContext directK8sInfraContext =
        DirectK8sInfraContext.builder()
            .namespace(namespace)
            .delegateId(delegateId)
            .connectorType(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS)
            .manualConfigAuthType(DirectK8sInfraContext.DirectК8sManualConfigAuthType.SERVICE_ACCOUNT)
            .masterUrl(masterUrl)
            .build();

    Optional<String> result = directK8sInfraContext.getConnectorInfo();
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo(
            "Kubernetes connector configured with masterUrl [https://masterurl.com:1234/] and credential type [ServiceAccount]");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToConnectorInfoManualCredentialsOpenIdConnect() {
    DirectK8sInfraContext directK8sInfraContext =
        DirectK8sInfraContext.builder()
            .namespace(namespace)
            .delegateId(delegateId)
            .connectorType(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS)
            .manualConfigAuthType(DirectK8sInfraContext.DirectК8sManualConfigAuthType.OPEN_ID_CONNECT)
            .masterUrl(masterUrl)
            .connectorDetails(format("IssuerURL: %s, Username: %s, ClientId: %s, Scopes: %s", oidcIssuerUrl,
                oidcUsername, oidcClientId, oidcScopes))
            .build();

    Optional<String> result = directK8sInfraContext.getConnectorInfo();
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo(
            "Kubernetes connector configured with masterUrl [https://masterurl.com:1234/] and credential type [OpenIdConnect] and details [IssuerURL: issuer_id, Username: oidc_user_name, ClientId: client_id, Scopes: scopes]");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testToConnectorInfoManualCredentialsInheritFromDelegate() {
    DirectK8sInfraContext directK8sInfraContext =
        DirectK8sInfraContext.builder()
            .namespace(namespace)
            .delegateId(delegateId)
            .connectorType(DirectK8sInfraContext.DirectК8sConnectorType.INHERIT_FROM_DELEGATE)
            .build();

    Optional<String> result = directK8sInfraContext.getConnectorInfo();
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo("Kubernetes connector with credentials inherited from delegate [delegate-id]");
  }
}
