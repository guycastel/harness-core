/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.k8s.trafficrouting;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;

@Singleton
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class K8sTrafficRoutingInfoDAL {
  @Inject private HPersistence persistence;

  public void saveK8sTrafficRoutingInfo(K8sTrafficRoutingInfo k8sTrafficRoutingInfo) {
    persistence.save(k8sTrafficRoutingInfo);
  }

  public K8sTrafficRoutingInfo getTrafficRoutingInfoForStageAndRelease(
      String accountId, String orgId, String projectId, String stageExecutionId, String releaseName) {
    Query<K8sTrafficRoutingInfo> query =
        persistence.createQuery(K8sTrafficRoutingInfo.class)
            .filter(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.accountId, accountId)
            .filter(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.orgId, orgId)
            .filter(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.projectId, projectId)
            .filter(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.stageExecutionId, stageExecutionId)
            .order(Sort.descending(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.createdAt));

    if (releaseName != null) {
      query.filter(K8sTrafficRoutingInfo.K8sTrafficRoutingInfoKeys.releaseName, releaseName);
    }
    return query.get();
  }
}
