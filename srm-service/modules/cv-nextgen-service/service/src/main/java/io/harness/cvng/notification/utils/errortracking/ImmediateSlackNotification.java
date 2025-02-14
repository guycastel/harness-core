/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking;

import static io.harness.cvng.notification.utils.NotificationRuleConstants.MONITORED_SERVICE_NAME;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.MONITORED_SERVICE_URL;
import static io.harness.cvng.notification.utils.errortracking.SavedFilterSlackNotification.SLACK_SAVED_SEARCH_FILTER_SECTION;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.ENVIRONMENT_NAME;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.EVENT_STATUS;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.NOTIFICATION_EVENT_TRIGGER_LIST;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.getNotificationUrl;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.validateTemplateValues;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.NOTIFICATION_NAME;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.NOTIFICATION_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.SLACK_EVENT_DETAILS_BUTTON;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.SLACK_FORMATTED_VERSION_LIST;

import io.harness.cvng.beans.errortracking.ErrorTrackingHitSummary;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.notification.entities.NotificationRule;
import io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification;
import io.harness.cvng.notification.utils.errortracking.interfaces.ImmediateNotification;

import java.util.HashMap;
import java.util.Map;

public class ImmediateSlackNotification {
  public static final String SLACK_EVENT_DETAILS_BUTTON_BLOCK_VALUE =
      "{\"type\": \"actions\",\"elements\": [{\"type\": \"button\",\"text\": {\"type\": \"plain_text\",\"text\": \"View Event Details\",\"emoji\": true},\"url\": \"${ARC_SCREEN_URL}\"}]}";

  public static Map<String, String> getNotificationDataMap(ErrorTrackingHitSummary errorTrackingHitSummary,
      String baseLinkUrl, MonitoredService monitoredService, NotificationRule notificationRule, String environmentId) {
    final Map<String, String> notificationDataMap = new HashMap<>();

    notificationDataMap.put(ENVIRONMENT_NAME, environmentId);
    notificationDataMap.put(EVENT_STATUS, ImmediateNotification.getEventStatus());
    notificationDataMap.put(NOTIFICATION_EVENT_TRIGGER_LIST, ImmediateNotification.getNotificationEventTriggerList());
    notificationDataMap.put(MONITORED_SERVICE_URL, getNotificationUrl(baseLinkUrl, monitoredService));
    notificationDataMap.put(MONITORED_SERVICE_NAME, monitoredService.getIdentifier());
    notificationDataMap.put(
        NOTIFICATION_URL, ErrorTrackingNotification.getNotificationUrl(baseLinkUrl, monitoredService));
    notificationDataMap.put(NOTIFICATION_NAME, notificationRule.getName());
    notificationDataMap.put(SLACK_SAVED_SEARCH_FILTER_SECTION, "");
    notificationDataMap.put(SLACK_FORMATTED_VERSION_LIST, getSlackFormattedVersionList(errorTrackingHitSummary));
    notificationDataMap.put(SLACK_EVENT_DETAILS_BUTTON,
        getSlackEventDetailsButton(errorTrackingHitSummary, baseLinkUrl, monitoredService, environmentId));

    validateTemplateValues(notificationDataMap, SLACK_SAVED_SEARCH_FILTER_SECTION);

    return notificationDataMap;
  }

  private static String getSlackEventDetailsButton(ErrorTrackingHitSummary errorTrackingHitSummary, String baseLinkUrl,
      MonitoredService monitoredService, String environmentId) {
    final String arcScreenUrl = ImmediateNotification.buildArcScreenUrlWithParameters(errorTrackingHitSummary,
        baseLinkUrl, monitoredService.getAccountId(), monitoredService.getOrgIdentifier(),
        monitoredService.getProjectIdentifier(), errorTrackingHitSummary.getRequestId(), environmentId,
        monitoredService.getServiceIdentifier(), errorTrackingHitSummary.getVersionId());

    return SLACK_EVENT_DETAILS_BUTTON_BLOCK_VALUE.replace("${ARC_SCREEN_URL}", arcScreenUrl);
  }

  private static String getSlackFormattedVersionList(ErrorTrackingHitSummary errorTrackingHitSummary) {
    StackTraceEvent stackTraceEvent = StackTraceEvent.builder()
                                          .version(errorTrackingHitSummary.getVersionId())
                                          .stackTrace(String.join(",", errorTrackingHitSummary.getStackTrace()))
                                          .build();
    return stackTraceEvent.toSlackString();
  }
}