/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.lock.PersistentLocker;
import io.harness.redis.RedisConfig;
import io.harness.utils.DebeziumFeatureFlagHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

/**
 *   This class manages the lifecycle of Debezium Controller threads.
 */
@Slf4j
@Singleton
public class DebeziumControllerStarter {
  @Inject @Named("DebeziumExecutorService") private ExecutorService debeziumExecutorService;
  @Inject private ChangeConsumerFactory consumerFactory;
  @Inject private DebeziumService debeziumService;
  @Inject private DebeziumFeatureFlagHelper featureFlagHelper;

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void startDebeziumController(DebeziumConfig debeziumConfig, ChangeConsumerConfig changeConsumerConfig,
      PersistentLocker locker, RedisConfig redisLockConfig, List<Integer> listOfErrorCodesForOffsetReset) {
    List<String> collections = debeziumConfig.getMonitoredCollections();
    for (String monitoredCollection : collections) {
      try {
        MongoCollectionChangeConsumer changeConsumer =
            consumerFactory.get(changeConsumerConfig, featureFlagHelper, monitoredCollection);
        DebeziumController debeziumController = new DebeziumController(
            DebeziumConfiguration.getDebeziumProperties(debeziumConfig, redisLockConfig, monitoredCollection),
            changeConsumer, locker, debeziumExecutorService, debeziumService, listOfErrorCodesForOffsetReset);
        debeziumExecutorService.submit(debeziumController);
        log.info("Starting Debezium Controller for Collection {} ...", monitoredCollection);
      } catch (Exception e) {
        log.error("Cannot Start Debezium Controller for Collection {}", monitoredCollection, e);
      }
    }
  }
}