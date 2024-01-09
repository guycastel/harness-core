/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.DebeziumFeatureFlagHelper;

import io.debezium.embedded.EmbeddedEngineChangeEvent;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.Header;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventsFrameworkChangeConsumerSnapshot extends EventsFrameworkChangeConsumer {
  public EventsFrameworkChangeConsumerSnapshot(ChangeConsumerConfig changeConsumerConfig,
      DebeziumFeatureFlagHelper featureFlagHelper, String collection, DebeziumProducerFactory producerFactory) {
    super(changeConsumerConfig, featureFlagHelper, collection, producerFactory);
  }
  @Override
  public void handleBatch(List<ChangeEvent<String, String>> records,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) throws InterruptedException {
    log.info("Handling a batch of {} records for collection {}", records.size(), collectionName);
    Collections.reverse(records);
    Map<String, ChangeEvent<String, String>> recordsMap = new HashMap<>();
    for (ChangeEvent<String, String> record : records) {
      if (!recordsMap.containsKey(record.key())) {
        recordsMap.put(record.key(), record);
      }
    }

    boolean debeziumEnabled = isDebeziumEnabled(collectionName);

    // Add the batch records to the stream(s)
    for (ChangeEvent<String, String> record : recordsMap.values()) {
      cnt++;
      if (debeziumEnabled) {
        process(record);
      }
      try {
        recordCommitter.markProcessed(record);
      } catch (InterruptedException e) {
        log.error("Exception Occurred while marking record as committed", e);
      }
    }
    recordCommitter.markBatchFinished();
  }

  private void process(ChangeEvent<String, String> record) {
    Optional<OpType> opType =
        getOperationType(((EmbeddedEngineChangeEvent<String, String, List<Header>>) record).sourceRecord());
    if (opType.isPresent()) {
      if (!OpType.SNAPSHOT.equals(opType.get())) {
        throw new InvalidRequestException("Snapshot completed");
      }
      DebeziumChangeEvent debeziumChangeEvent = DebeziumChangeEvent.newBuilder()
                                                    .setKey(getKeyOrDefault(record))
                                                    .setValue(getValueOrDefault(record))
                                                    .setOptype(opType.get().toString())
                                                    .setTimestamp(System.currentTimeMillis())
                                                    .build();
      Producer producer = producerFactory.get(record.destination(), redisStreamSize, mode, configuration);
      producer.send(Message.newBuilder().setData(debeziumChangeEvent.toByteString()).build());
    }
  }
}
