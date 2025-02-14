/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.user.NGRemoveUserFilter;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.utils.ScopeUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Slf4j
@Singleton
public class UserMembershipReconciliationMessageProcessor implements MessageListener {
  private final NgUserService ngUserService;

  @Inject
  public UserMembershipReconciliationMessageProcessor(NgUserService ngUserService) {
    this.ngUserService = ngUserService;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap == null || metadataMap.get(ENTITY_TYPE) == null || metadataMap.get(ACTION) == null) {
        return true;
      }
      if (!DELETE_ACTION.equals(metadataMap.get(ACTION))) {
        return true;
      }
      String entityType = metadataMap.get(ENTITY_TYPE);
      switch (entityType) {
        case ACCOUNT_ENTITY:
          return processAccountDeleteEvent(message);
        case ORGANIZATION_ENTITY:
          return processOrgDeleteEvent(message);
        case PROJECT_ENTITY:
          return processProjectDeleteEvent(message);
        default:
          return true;
      }
    }
    return true;
  }

  private boolean processAccountDeleteEvent(Message message) {
    AccountEntityChangeDTO accountEntityChangeDTO;
    try {
      accountEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking AccountEntityChangeDTO for key %s", message.getId()), e);
    }
    return processScopeDelete(Scope.of(StringUtils.stripToNull(accountEntityChangeDTO.getAccountId()), null, null));
  }

  private boolean processOrgDeleteEvent(Message message) {
    OrganizationEntityChangeDTO organizationEntityChangeDTO;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    return processScopeDelete(Scope.of(StringUtils.stripToNull(organizationEntityChangeDTO.getAccountIdentifier()),
        StringUtils.stripToNull(organizationEntityChangeDTO.getIdentifier()), null));
  }

  private boolean processProjectDeleteEvent(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception is unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    return processScopeDelete(Scope.of(StringUtils.stripToNull(projectEntityChangeDTO.getAccountIdentifier()),
        StringUtils.stripToNull(projectEntityChangeDTO.getOrgIdentifier()),
        StringUtils.stripToNull(projectEntityChangeDTO.getIdentifier())));
  }

  private boolean processScopeDelete(Scope scope) {
    List<String> userIds = ngUserService.listUserIds(scope);
    AtomicBoolean success = new AtomicBoolean(true);
    userIds.forEach(userId -> {
      if (!ngUserService.removeUserFromScope(
              userId, scope, UserMembershipUpdateSource.SYSTEM, NGRemoveUserFilter.STRICTLY_FORCE_REMOVE_USER)) {
        log.error("Delete operation failed for users with at scope [{}]", ScopeUtils.toString(scope));
        success.set(false);
      }
    });
    if (success.get()) {
      log.info("Successfully completed deletion for users at scope [{}]", ScopeUtils.toString(scope));
    }
    return success.get();
  }
}
