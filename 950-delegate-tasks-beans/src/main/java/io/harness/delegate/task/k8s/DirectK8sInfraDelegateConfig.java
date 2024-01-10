/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.k8s;

import static java.lang.String.format;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.taskcontext.infra.DirectK8sInfraContext;
import io.harness.taskcontext.infra.DirectK8sInfraContext.DirectK8sInfraContextBuilder;
import io.harness.taskcontext.infra.InfraContext;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@RecasterAlias("io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig")
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
@Slf4j
public class DirectK8sInfraDelegateConfig implements K8sInfraDelegateConfig {
  String namespace;
  KubernetesClusterConfigDTO kubernetesClusterConfigDTO;
  List<EncryptedDataDetail> encryptionDataDetails;
  boolean useSocketCapability;

  @Override
  public boolean useSocketCapability() {
    return useSocketCapability;
  }

  @Override
  public InfraContext toInfraContext(String delegateId) {
    DirectK8sInfraContextBuilder directK8sInfraContextBuilder = DirectK8sInfraContext.builder();
    try {
      directK8sInfraContextBuilder.delegateId(delegateId).namespace(namespace);

      switch (kubernetesClusterConfigDTO.getCredential().getKubernetesCredentialType()) {
        case MANUAL_CREDENTIALS: {
          KubernetesClusterDetailsDTO kubernetesClusterDetailsDTO =
              (KubernetesClusterDetailsDTO) kubernetesClusterConfigDTO.getCredential().getConfig();
          KubernetesAuthDTO kubernetesAuthDTO = kubernetesClusterDetailsDTO.getAuth();

          directK8sInfraContextBuilder.connectorType(DirectK8sInfraContext.DirectК8sConnectorType.MANUAL_CREDENTIALS)
              .masterUrl(kubernetesClusterDetailsDTO.getMasterUrl());

          switch (kubernetesAuthDTO.getAuthType()) {
            case USER_PASSWORD: {
              KubernetesUserNamePasswordDTO kubernetesAuthCredentials =
                  (KubernetesUserNamePasswordDTO) kubernetesAuthDTO.getCredentials();
              directK8sInfraContextBuilder
                  .manualConfigAuthType(DirectK8sInfraContext.DirectК8sManualConfigAuthType.USER_PASSWORD)
                  .connectorDetails(format("Username: %s", kubernetesAuthCredentials.getUsername()));
              break;
            }
            case CLIENT_KEY_CERT: {
              directK8sInfraContextBuilder.manualConfigAuthType(
                  DirectK8sInfraContext.DirectК8sManualConfigAuthType.CLIENT_KEY_CERT);
              break;
            }
            case SERVICE_ACCOUNT: {
              directK8sInfraContextBuilder.manualConfigAuthType(
                  DirectK8sInfraContext.DirectК8sManualConfigAuthType.SERVICE_ACCOUNT);
              break;
            }
            case OPEN_ID_CONNECT: {
              KubernetesOpenIdConnectDTO kubernetesAuthCredentials =
                  (KubernetesOpenIdConnectDTO) kubernetesAuthDTO.getCredentials();
              directK8sInfraContextBuilder
                  .manualConfigAuthType(DirectK8sInfraContext.DirectК8sManualConfigAuthType.OPEN_ID_CONNECT)
                  .connectorDetails(format("IssuerURL: %s, Username: %s, ClientId: %s, Scopes: %s",
                      kubernetesAuthCredentials.getOidcIssuerUrl(), kubernetesAuthCredentials.getOidcUsername(),
                      String.valueOf(kubernetesAuthCredentials.getOidcClientIdRef().getDecryptedValue()),
                      kubernetesAuthCredentials.getOidcScopes()));
              break;
            }
            default: {
              break;
            }
          }
          break;
        }
        case INHERIT_FROM_DELEGATE: {
          directK8sInfraContextBuilder.connectorType(
              DirectK8sInfraContext.DirectК8sConnectorType.INHERIT_FROM_DELEGATE);
          break;
        }
        default: {
          break;
        }
      }

    } catch (Exception e) {
      log.error("Failed to create Direct K8s InfraContext for task context object", e);
    }

    return directK8sInfraContextBuilder.build();
  }
}
