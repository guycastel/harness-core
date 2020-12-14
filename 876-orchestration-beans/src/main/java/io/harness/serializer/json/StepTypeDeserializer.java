package io.harness.serializer.json;

import io.harness.pms.contracts.steps.StepType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public class StepTypeDeserializer extends JsonDeserializer<StepType> {
  @Override
  public StepType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectNode root = p.readValueAsTree();
    return StepType.newBuilder().setType(root.get("type").asText()).build();
  }
}
