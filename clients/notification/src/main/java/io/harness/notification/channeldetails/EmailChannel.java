/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.channeldetails;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.util.Collections.emptyMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.notification.NotificationRequest;
import io.harness.notification.Team;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@EqualsAndHashCode(callSuper = true)
public class EmailChannel extends NotificationChannel {
  List<String> recipients;
  List<String> ccEmailIds;
  String subject;
  String body;

  @Builder
  public EmailChannel(String accountId, List<NotificationRequest.UserGroup> userGroups, String templateId,
      Map<String, String> templateData, Team team, List<String> recipients, List<String> ccEmailIds, String subject,
      String body) {
    super(accountId, userGroups, templateId, templateData, team);
    this.recipients = recipients;
    this.ccEmailIds = ccEmailIds;
    this.subject = subject;
    this.body = body;
  }

  @Override
  public NotificationRequest buildNotificationRequest() {
    NotificationRequest.Builder builder = NotificationRequest.newBuilder();
    String notificationId = generateUuid();
    return builder.setId(notificationId).setAccountId(accountId).setTeam(team).setEmail(buildEmail(builder)).build();
  }

  private NotificationRequest.Email.Builder buildEmail(NotificationRequest.Builder builder) {
    NotificationRequest.Email.Builder emailBuilder =
        builder.getEmailBuilder()
            .addAllEmailIds(recipients)
            .putAllTemplateData(isNotEmpty(templateData) ? templateData : emptyMap())
            .addAllUserGroup(CollectionUtils.emptyIfNull(userGroups));

    if (isNotEmpty(ccEmailIds)) {
      emailBuilder.addAllCcEmailIds(ccEmailIds);
    }
    if (isNotEmpty(subject)) {
      emailBuilder.setSubject(subject);
    }
    if (isNotEmpty(body)) {
      emailBuilder.setBody(body);
    }
    if (isNotEmpty(templateId)) {
      emailBuilder.setTemplateId(templateId);
    }
    return emailBuilder;
  }
}
