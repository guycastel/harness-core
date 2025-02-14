/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.gcp.request;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpOidcDetailsDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.oidc.gcp.delegate.GcpOidcTokenExchangeDetailsForDelegate;
import io.harness.security.encryption.EncryptedDataDetail;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@Data
@SuperBuilder
@OwnedBy(CDP)
public abstract class GcpRequest extends ConnectorTaskParams implements ExecutionCapabilityDemander {
  private boolean useDelegate;
  // Below 2 are NG specific.
  private List<EncryptedDataDetail> encryptionDetails;
  private GcpManualDetailsDTO gcpManualDetailsDTO;
  private GcpOidcDetailsDTO gcpOidcDetailsDTO;
  private GcpOidcTokenExchangeDetailsForDelegate gcpOidcTokenExchangeDetailsForDelegate;

  public Duration getExecutionTimeout() {
    return Duration.ofSeconds(30);
  }
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (isNotEmpty(delegateSelectors)) {
      return singletonList(SelectorCapability.builder().selectors(delegateSelectors).build());
    }
    return emptyList();
  }
}
