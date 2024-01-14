/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.settings.jobs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.settings.service.BackstagePermissionsService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.IDP)
public class BackstagePermissionsSyncJob implements Managed {
  private static final long DELAY_IN_MINUTES = TimeUnit.HOURS.toMinutes(24);
  private ScheduledExecutorService executorService;
  private final BackstagePermissionsService backstagePermissionsService;
  private final NamespaceService namespaceService;

  @Inject
  public BackstagePermissionsSyncJob(@Named("backstagePermissionsSyncer") ScheduledExecutorService executorService,
      BackstagePermissionsService backstagePermissionsService, NamespaceService namespaceService) {
    this.executorService = executorService;
    this.backstagePermissionsService = backstagePermissionsService;
    this.namespaceService = namespaceService;
  }

  @Override
  public void start() throws Exception {
    executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("backstage-permissions-sync-job").build());
    executorService.scheduleWithFixedDelay(this::run, 0, DELAY_IN_MINUTES, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdownNow();
    executorService.awaitTermination(30, TimeUnit.SECONDS);
  }

  public void run() {
    log.info("Backstage permissions sync job started");
    List<String> accountIds = namespaceService.getAccountIds();
    accountIds.forEach(account -> {
      try {
        backstagePermissionsService.findAndSyncPermissions(account);
      } catch (Exception e) {
        log.error("Could not sync backstage permissions for account {}", account, e);
      }
    });
    log.info("Backstage permissions sync job completed");
  }
}
