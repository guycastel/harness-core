/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.exception.InvalidRequestException;
import io.harness.utils.DebeziumFeatureFlagHelper;

import com.google.inject.Inject;

public class ChangeConsumerFactory {
  @Inject private DebeziumProducerFactory producerFactory;

  @SuppressWarnings("unchecked")
  public <T extends MongoCollectionChangeConsumer> T get(
      ChangeConsumerConfig changeConsumerConfig, DebeziumFeatureFlagHelper featureFlagHelper, String collection) {
    ConsumerType consumerType = changeConsumerConfig.getConsumerType();
    if (consumerType != null) {
      switch (consumerType) {
        case EVENTS_FRAMEWORK:
          if (changeConsumerConfig.getConsumerMode() == ConsumerMode.SNAPSHOT) {
            return (T) new EventsFrameworkChangeConsumerSnapshot(
                changeConsumerConfig, featureFlagHelper, collection, producerFactory);
          } else {
            return (T) new EventsFrameworkChangeConsumerStreaming(
                changeConsumerConfig, featureFlagHelper, collection, producerFactory);
          }
        default:
          throw new InvalidRequestException("Change Consumer not Supported for " + consumerType);
      }
    } else {
      throw new InvalidRequestException("Consumer Type is null");
    }
  }
}
