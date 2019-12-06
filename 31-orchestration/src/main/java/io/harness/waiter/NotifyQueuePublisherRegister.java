package io.harness.waiter;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class NotifyQueuePublisherRegister {
  Map<String, NotifyQueuePublisher> registry = new HashMap<>();

  public void register(String name, NotifyQueuePublisher publisher) {
    registry.put(name, publisher);
  }

  public NotifyQueuePublisher obtain(String publisherName) {
    return registry.get(publisherName);
  }
}
