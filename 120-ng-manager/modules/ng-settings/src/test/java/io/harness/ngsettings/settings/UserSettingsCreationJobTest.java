/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.settings;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngsettings.SettingCategory.USER;
import static io.harness.rule.OwnerRule.SAHIBA;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ngsettings.NgSettingsTestBase;
import io.harness.ngsettings.SettingPlanConfig;
import io.harness.ngsettings.SettingValueType;
import io.harness.ngsettings.entities.SettingsConfigurationState;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.reflection.ReflectionUtils;
import io.harness.repositories.ngsettings.custom.ConfigurationStateRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import io.serializer.HObjectMapper;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
public class UserSettingsCreationJobTest extends NgSettingsTestBase {
  private static final String NEW_SETTING = "new_setting_identifier";
  private static final String SETTING_NAME = "new_setting_name";
  @Inject private UserSettingsService userSettingsService;
  @Inject private ConfigurationStateRepository configurationStateRepository;
  @Inject private UserSettingsCreationJob userSettingsCreationJob;
  @Inject PersistentLocker persistentLocker;
  private static final String USER_SETTINGS_CONFIG_FIELD = "userSettingsConfig";
  private static final String VERSION_FIELD = "version";
  private static final String lockName =
      String.format("%s_userSettingConfigurationsLock", UserSettingsCreationJob.class.getName());

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testSettingsValidation() {
    UserSettingsCreationJob userSettingsCreationJob1 =
        new UserSettingsCreationJob(userSettingsService, persistentLocker, configurationStateRepository);
    UserSettingsConfig userSettingsConfig =
        (UserSettingsConfig) ReflectionUtils.getFieldValue(userSettingsCreationJob1, USER_SETTINGS_CONFIG_FIELD);
    assertNotNull(userSettingsConfig);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testUserSettingCreationJobRun() {
    UserSettingsConfig userSettingsConfig =
        (UserSettingsConfig) ReflectionUtils.getFieldValue(userSettingsCreationJob, USER_SETTINGS_CONFIG_FIELD);
    when(persistentLocker.waitToAcquireLockOptional(eq(lockName), notNull(), notNull()))
        .thenReturn(mock(AcquiredLock.class));
    userSettingsCreationJob.run();
    validate(userSettingsConfig);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testUserSettingCreationJobRunWithUpdate() throws NoSuchFieldException, IllegalAccessException {
    Field f = userSettingsCreationJob.getClass().getDeclaredField(USER_SETTINGS_CONFIG_FIELD);
    UserSettingsConfig userSettingsConfig =
        (UserSettingsConfig) ReflectionUtils.getFieldValue(userSettingsCreationJob, USER_SETTINGS_CONFIG_FIELD);
    ReflectionUtils.setObjectField(
        userSettingsConfig.getClass().getDeclaredField(VERSION_FIELD), userSettingsConfig, 2);
    UserSettingsConfig latestUserSettingsConfig = (UserSettingsConfig) HObjectMapper.clone(userSettingsConfig);
    UserSettingsConfig currentUserSettingsConfig = UserSettingsConfig.builder()
                                                       .version(1)
                                                       .name(latestUserSettingsConfig.getName())
                                                       .userSettings(new HashSet<>())
                                                       .build();
    ReflectionUtils.setObjectField(f, userSettingsCreationJob, currentUserSettingsConfig);
    when(persistentLocker.waitToAcquireLockOptional(eq(lockName), notNull(), notNull()))
        .thenReturn(mock(AcquiredLock.class));
    userSettingsCreationJob.run();
    validate(currentUserSettingsConfig);
    ReflectionUtils.setObjectField(f, userSettingsCreationJob, latestUserSettingsConfig);
    userSettingsCreationJob.run();
    validate(latestUserSettingsConfig);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testAddingNewUserSetting() throws NoSuchFieldException, IllegalAccessException {
    userSettingsCreationJob.run();
    UserSettingsConfig userSettingsConfig =
        (UserSettingsConfig) ReflectionUtils.getFieldValue(userSettingsCreationJob, USER_SETTINGS_CONFIG_FIELD);
    Set<UserSettingConfiguration> currentUserSettings = userSettingsConfig.getUserSettings();
    Set<ScopeLevel> allowedScopes = new HashSet<>();
    allowedScopes.add(ScopeLevel.ACCOUNT);
    currentUserSettings.add(UserSettingConfiguration.builder()
                                .identifier(NEW_SETTING)
                                .name(SETTING_NAME)
                                .allowedScopes(allowedScopes)
                                .valueType(SettingValueType.STRING)
                                .category(USER)
                                .build());
    int currentVersion = userSettingsConfig.getVersion();
    ReflectionUtils.setObjectField(
        userSettingsConfig.getClass().getDeclaredField(VERSION_FIELD), userSettingsConfig, currentVersion + 1);
    when(persistentLocker.waitToAcquireLockOptional(eq(lockName), notNull(), notNull()))
        .thenReturn(mock(AcquiredLock.class));
    userSettingsCreationJob.run();
    validate(userSettingsConfig);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testRemovingNewUserSetting() throws NoSuchFieldException, IllegalAccessException {
    Field f = userSettingsCreationJob.getClass().getDeclaredField(USER_SETTINGS_CONFIG_FIELD);
    UserSettingsConfig currentUserSettingsConfig =
        (UserSettingsConfig) ReflectionUtils.getFieldValue(userSettingsCreationJob, USER_SETTINGS_CONFIG_FIELD);
    UserSettingsConfig latestUserSettingsConfig = (UserSettingsConfig) HObjectMapper.clone(currentUserSettingsConfig);
    ReflectionUtils.setObjectField(latestUserSettingsConfig.getClass().getDeclaredField(VERSION_FIELD),
        latestUserSettingsConfig, currentUserSettingsConfig.getVersion() + 1);
    Set<ScopeLevel> allowedScopes = new HashSet<>();
    allowedScopes.add(ScopeLevel.ACCOUNT);
    currentUserSettingsConfig.getUserSettings().add(UserSettingConfiguration.builder()
                                                        .identifier(NEW_SETTING)
                                                        .name(SETTING_NAME)
                                                        .allowedScopes(allowedScopes)
                                                        .valueType(SettingValueType.STRING)
                                                        .category(USER)
                                                        .build());
    when(persistentLocker.waitToAcquireLockOptional(eq(lockName), notNull(), notNull()))
        .thenReturn(mock(AcquiredLock.class));
    userSettingsCreationJob.run();
    validate(currentUserSettingsConfig);

    ReflectionUtils.setObjectField(f, userSettingsCreationJob, latestUserSettingsConfig);
    userSettingsCreationJob.run();
    validate(latestUserSettingsConfig);
  }

  public void validate(UserSettingsConfig userSettingsConfig) {
    Optional<SettingsConfigurationState> optional =
        configurationStateRepository.getByIdentifier(userSettingsConfig.getName());
    assertTrue(optional.isPresent());
    assertEquals(userSettingsConfig.getVersion(), optional.get().getConfigVersion());
    List<UserSettingConfiguration> currentUserSettingConfigurations = userSettingsService.listDefaultSettings();
    if (currentUserSettingConfigurations.size() != userSettingsConfig.getUserSettings().size()) {
      fail("The count of setting configurations does not match");
    }
    assertThat(currentUserSettingConfigurations)
        .usingElementComparatorIgnoringFields("allowedPlans")
        .containsAll(userSettingsConfig.getUserSettings());
    Map<String, UserSettingConfiguration> userSettingConfigurationMap = new HashMap<>();
    userSettingsConfig.getUserSettings().forEach(userSettingConfiguration
        -> userSettingConfigurationMap.put(userSettingConfiguration.getIdentifier(), userSettingConfiguration));

    currentUserSettingConfigurations.forEach(userSettingConfiguration -> {
      if (isNotEmpty(userSettingConfiguration.getAllowedPlans())) {
        Map<Edition, SettingPlanConfig> allowedPlansFromConfig =
            userSettingConfigurationMap.get(userSettingConfiguration.getIdentifier()).getAllowedPlans();
        Map<Edition, SettingPlanConfig> allowedPlansFromDB = userSettingConfiguration.getAllowedPlans();
        allowedPlansFromConfig.forEach((edition, settingPlanConfig) -> {
          assertEquals(settingPlanConfig.getEditable(), allowedPlansFromDB.get(edition).getEditable());
          assertEquals(settingPlanConfig.getDefaultValue(), allowedPlansFromDB.get(edition).getDefaultValue());
        });
      }
    });
  }
}
