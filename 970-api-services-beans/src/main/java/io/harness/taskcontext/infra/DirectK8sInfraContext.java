/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.taskcontext.infra;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
@Slf4j
public class DirectK8sInfraContext implements InfraContext {
  String delegateId;
  String namespace;
  DirectК8sConnectorType connectorType;
  String masterUrl;
  DirectК8sManualConfigAuthType manualConfigAuthType;
  String connectorDetails;

  @Override
  public Optional<String> getConnectorInfo() {
    try {
      switch (connectorType) {
        case MANUAL_CREDENTIALS: {
          String info = "Kubernetes connector configured with masterUrl [%s] and credential type [%s]";
          if (DirectК8sManualConfigAuthType.USER_PASSWORD.equals(manualConfigAuthType)
              || DirectК8sManualConfigAuthType.OPEN_ID_CONNECT.equals(manualConfigAuthType)) {
            return Optional.of(
                format(format("%s %s", info, "and details [%s]"), masterUrl, manualConfigAuthType, connectorDetails));
          }
          return Optional.of(format(info, masterUrl, manualConfigAuthType));
        }
        case INHERIT_FROM_DELEGATE: {
          return Optional.of(format("Kubernetes connector with credentials inherited from delegate [%s]", delegateId));
        }
        default: {
          return Optional.empty();
        }
      }
    } catch (Exception e) {
      log.error("Failed to generate DirectK8s ConnectorInfo for task context", e);
    }

    return Optional.empty();
  }

  public enum DirectК8sConnectorType {
    INHERIT_FROM_DELEGATE("InheritFromDelegate"),
    MANUAL_CREDENTIALS("ManualConfig");

    private final String value;

    DirectК8sConnectorType(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  public enum DirectК8sManualConfigAuthType {
    USER_PASSWORD("UsernamePassword"),
    CLIENT_KEY_CERT("ClientKeyCert"),
    SERVICE_ACCOUNT("ServiceAccount"),
    OPEN_ID_CONNECT("OpenIdConnect");

    private final String value;

    DirectК8sManualConfigAuthType(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }
}
