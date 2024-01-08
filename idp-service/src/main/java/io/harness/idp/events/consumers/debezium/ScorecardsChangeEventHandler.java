/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.events.consumers.debezium;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventHandler.DebeziumAbstractRedisEventHandler;
import io.harness.timescaledb.Tables;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class ScorecardsChangeEventHandler extends DebeziumAbstractRedisEventHandler {
  @Inject private DSLContext dsl;

  @Override
  public boolean handleCreateEvent(String id, String value) {
    Record insertRecord = createRecord(id, value);
    if (insertRecord == null) {
      return true;
    }
    try {
      upsert(insertRecord);
      log.debug("Successfully inserted data in scorecards for id {}", id);
    } catch (DataAccessException ex) {
      log.error("Caught exception while inserting data in scorecards for id {}", id, ex);
      return false;
    }
    return false;
  }

  @Override
  public boolean handleDeleteEvent(String id) {
    try {
      dsl.delete(Tables.SCORECARDS).where(Tables.SCORECARDS.ID.eq(id)).execute();
      log.debug("Successfully deleted data in scorecards for id {}", id);
    } catch (DataAccessException ex) {
      log.error("Caught exception while deleting data in scorecards for id {}", id, ex);
      return false;
    }
    return true;
  }

  @Override
  public boolean handleUpdateEvent(String id, String value) {
    Record updateRecord = createRecord(id, value);
    if (updateRecord == null) {
      return true;
    }
    try {
      upsert(updateRecord);
      log.debug("Successfully updated data in scorecards for id {}", id);
    } catch (DataAccessException ex) {
      log.error("Caught Exception while updating data in scorecards for id {}", id, ex);
      return false;
    }
    return true;
  }

  @SneakyThrows
  public Record createRecord(String id, String value) {
    JsonNode node = objectMapper.readTree(value);

    Record createRecord = dsl.newRecord(Tables.SCORECARDS);
    createRecord.set(Tables.SCORECARDS.ID, id);

    populateFromRoot(node, createRecord);
    populateFromFilter(node, createRecord);
    populateFromChecks(node, createRecord);

    return createRecord;
  }

  private void populateFromRoot(JsonNode node, Record createRecord) {
    if (node.get("accountIdentifier") != null) {
      createRecord.set(Tables.SCORECARDS.ACCOUNT_IDENTIFIER, node.get("accountIdentifier").asText());
    }

    if (node.get("identifier") != null) {
      createRecord.set(Tables.SCORECARDS.IDENTIFIER, node.get("identifier").asText());
    }

    if (node.get("name") != null) {
      createRecord.set(Tables.SCORECARDS.NAME, node.get("name").asText());
    }

    if (node.get("description") != null) {
      createRecord.set(Tables.SCORECARDS.DESCRIPTION, node.get("description").asText());
    }

    if (node.get("weightageStrategy") != null) {
      createRecord.set(Tables.SCORECARDS.WEIGHTAGE_STRATEGY, node.get("weightageStrategy").asText());
    }

    if (node.get("published") != null) {
      createRecord.set(Tables.SCORECARDS.PUBLISHED, node.get("published").asBoolean());
    }

    if (node.get("isDeleted") != null) {
      createRecord.set(Tables.SCORECARDS.DELETED, node.get("isDeleted").asBoolean());
    }

    if (node.get("createdAt") != null) {
      createRecord.set(Tables.SCORECARDS.CREATED_AT, node.get("createdAt").asLong());
    }

    if (node.get("createdBy") != null) {
      JsonNode createdByNode = node.get("createdBy");
      if (createdByNode.get("name") != null) {
        createRecord.set(Tables.SCORECARDS.CREATED_BY, createdByNode.get("name").asText());
      }
    }

    if (node.get("lastUpdatedAt") != null) {
      createRecord.set(Tables.SCORECARDS.LAST_UPDATED_AT, node.get("lastUpdatedAt").asLong());
    }

    if (node.get("lastUpdatedBy") != null) {
      JsonNode lastUpdatedByNode = node.get("lastUpdatedBy");
      if (lastUpdatedByNode.get("name") != null) {
        createRecord.set(Tables.SCORECARDS.LAST_UPDATED_BY, lastUpdatedByNode.get("name").asText());
      }
    }
  }

  private void populateFromFilter(JsonNode node, Record createRecord) {
    if (node.get("filter") != null) {
      JsonNode nodeFilter = node.get("filter");
      String filter = nodeFilter.get("kind").asText() + " | " + nodeFilter.get("type").asText();
      List<String> owners = nodeFilter.get("owners") != null
          ? StreamSupport.stream(nodeFilter.get("owners").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList())
          : new ArrayList<>();
      if (isNotEmpty(owners)) {
        filter = filter + " | " + String.join(", ", owners);
      }
      List<String> tags = nodeFilter.get("tags") != null
          ? StreamSupport.stream(nodeFilter.get("tags").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList())
          : new ArrayList<>();
      if (isNotEmpty(tags)) {
        filter = filter + " | " + String.join(", ", tags);
      }
      List<String> lifecycle = nodeFilter.get("lifecycle") != null
          ? StreamSupport.stream(nodeFilter.get("lifecycle").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList())
          : new ArrayList<>();
      if (isNotEmpty(lifecycle)) {
        filter = filter + " | " + String.join(", ", lifecycle);
      }
      createRecord.set(Tables.SCORECARDS.FILTER, filter);
    }
  }

  private void populateFromChecks(JsonNode node, Record createRecord) {
    if (node.get("checks") != null) {
      JsonNode nodeChecks = node.get("checks");
      List<JsonNode> checks = StreamSupport.stream(nodeChecks.spliterator(), false).collect(Collectors.toList());
      createRecord.set(Tables.SCORECARDS.TOTAL_NUMBER_OF_CHECKS, (short) checks.size());
      List<JsonNode> customChecks =
          checks.stream().filter(jsonNode -> jsonNode.get("isCustom").asBoolean()).collect(Collectors.toList());
      createRecord.set(Tables.SCORECARDS.NUMBER_OF_CUSTOM_CHECKS, (short) customChecks.size());
    }
  }

  private void upsert(Record upsertRecord) {
    dsl.insertInto(Tables.SCORECARDS)
        .set(upsertRecord)
        .onConflict(Tables.SCORECARDS.ID)
        .doUpdate()
        .set(upsertRecord)
        .execute();
  }
}
