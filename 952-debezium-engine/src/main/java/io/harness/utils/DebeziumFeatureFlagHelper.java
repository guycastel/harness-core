/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import io.harness.account.AccountClient;
import io.harness.beans.FeatureName;
import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.dto.Target;
import io.harness.remote.client.CGRestUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import javax.ejb.Singleton;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DebeziumFeatureFlagHelper {
  @Inject Provider<CfClient> cfClient;
  @Inject private AccountClient accountClient;
  private static final String DEPLOY_MODE = System.getenv("DEPLOY_MODE");
  private static final String DEPLOY_VERSION = System.getenv("DEPLOY_VERSION");

  public boolean isEnabled(@NotEmpty String target, @NotNull FeatureName feature) {
    try {
      return isFlagEnabledForAccountId(target, feature.toString());
    } catch (Exception e) {
      log.error(String.format("Error getting feature flag %s for given account: %s", feature, target));
      return false;
    }
  }

  private boolean isFlagEnabledForAccountId(String target, String featureName) {
    if (checkIfEnvOnPremOrCommunity()) {
      return CGRestUtils.getResponse(accountClient.isFeatureFlagEnabled(featureName, target));
    }
    return cfClient.get().boolVariation(featureName, Target.builder().identifier(target).build(), false);
  }

  private boolean checkIfEnvOnPremOrCommunity() {
    return (DEPLOY_MODE != null && (DEPLOY_MODE.equals("ONPREM") || DEPLOY_MODE.equals("KUBERNETES_ONPREM")))
        || (DEPLOY_VERSION != null && DEPLOY_VERSION.equals("COMMUNITY"));
  }
}
