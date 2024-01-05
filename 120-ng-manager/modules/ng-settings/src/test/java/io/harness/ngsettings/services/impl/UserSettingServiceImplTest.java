/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.services.impl;

import static io.harness.ngsettings.SettingConstants.GLOBAL_ACCOUNT;
import static io.harness.rule.OwnerRule.SAHIBA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngsettings.SettingUpdateType;
import io.harness.ngsettings.SettingValueType;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.mapper.UserSettingMapper;
import io.harness.repositories.ngsettings.spring.SettingRepository;
import io.harness.repositories.ngsettings.spring.UserSettingConfigurationRepository;
import io.harness.repositories.ngsettings.spring.UserSettingRepository;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UserSettingServiceImplTest extends CategoryTest {
  @Mock UserSettingConfigurationRepository userSettingConfigurationRepository;
  @Mock SettingRepository settingRepository;

  @Mock UserSettingRepository userSettingRepository;

  @Mock UserSettingMapper userSettingMapper;
  @InjectMocks private UserSettingsServiceImpl userSettingsService;
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testGetForDefaultValueOfUserSetting() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    UserSettingConfiguration defaultUserSetting = new UserSettingConfiguration();
    defaultUserSetting.setIdentifier(identifier);
    defaultUserSetting.setValueType(SettingValueType.BOOLEAN);
    defaultUserSetting.setDefaultValue("false");
    List<UserSettingConfiguration> defaultUserSettings = Arrays.asList(UserSettingConfiguration.builder()
                                                                           .identifier(identifier)
                                                                           .valueType(SettingValueType.STRING)
                                                                           .defaultValue("default1")
                                                                           .build());
    when(userSettingConfigurationRepository.findAll(any())).thenReturn(defaultUserSettings);

    when(userSettingRepository.getUserSettingByIdentifier(any(), any(), any())).thenReturn(null);
    when(userSettingConfigurationRepository.findByIdentifier(identifier)).thenReturn(defaultUserSetting);
    SettingValueResponseDTO result = userSettingsService.get(identifier, accountIdentifier, userId);
    assertThat(result).isNotNull();
    assertThat(result.getValueType()).isEqualTo(SettingValueType.BOOLEAN);
    assertThat(result.getValue()).isEqualTo("false");
    verify(userSettingRepository, times(2)).getUserSettingByIdentifier(any(), any(), any());
    verify(userSettingConfigurationRepository, times(1)).findByIdentifier(identifier);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testGetForValueWithGlobalAccountForUserSetting() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);

    UserSetting globalUserSetting = UserSetting.builder().build();
    globalUserSetting.setIdentifier(identifier);
    globalUserSetting.setValueType(SettingValueType.BOOLEAN);
    globalUserSetting.setValue("true");

    List<UserSettingConfiguration> defaultUserSettings = Arrays.asList(UserSettingConfiguration.builder()
                                                                           .identifier(identifier)
                                                                           .valueType(SettingValueType.STRING)
                                                                           .defaultValue("default1")
                                                                           .build());
    when(userSettingConfigurationRepository.findAll(any())).thenReturn(defaultUserSettings);

    when(userSettingRepository.getUserSettingByIdentifier(any(), any(), any())).thenReturn(null);
    when(userSettingRepository.getUserSettingByIdentifier(eq(GLOBAL_ACCOUNT), any(), any()))
        .thenReturn(globalUserSetting);

    SettingValueResponseDTO result = userSettingsService.get(identifier, accountIdentifier, userId);

    assertThat(result).isNotNull();
    assertThat(result.getValueType()).isEqualTo(SettingValueType.BOOLEAN);
    assertThat(result.getValue()).isEqualTo("true");

    verify(userSettingRepository, times(2)).getUserSettingByIdentifier(any(), any(), any());
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testGetForValueWithAccountForUserSetting() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    List<UserSettingConfiguration> defaultUserSettings = Arrays.asList(UserSettingConfiguration.builder()
                                                                           .identifier(identifier)
                                                                           .valueType(SettingValueType.STRING)
                                                                           .defaultValue("default1")
                                                                           .build());
    UserSetting userSettingForAccount = UserSetting.builder().build();
    userSettingForAccount.setIdentifier(identifier);
    userSettingForAccount.setValueType(SettingValueType.BOOLEAN);
    userSettingForAccount.setValue("true");
    when(userSettingConfigurationRepository.findAll(any())).thenReturn(defaultUserSettings);
    when(userSettingRepository.getUserSettingByIdentifier(any(), any(), any())).thenReturn(null);
    when(userSettingRepository.getUserSettingByIdentifier(eq(GLOBAL_ACCOUNT), any(), any()))
        .thenReturn(userSettingForAccount);

    SettingValueResponseDTO result = userSettingsService.get(identifier, accountIdentifier, userId);

    assertThat(result).isNotNull();
    assertThat(result.getValueType()).isEqualTo(SettingValueType.BOOLEAN);
    assertThat(result.getValue()).isEqualTo("true");

    verify(userSettingRepository, times(2)).getUserSettingByIdentifier(any(), any(), any());
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testListUserSettings() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);

    List<UserSetting> userSettingForAccount =
        Arrays.asList(UserSetting.builder().identifier("setting1").value("value1").build(),
            UserSetting.builder().identifier("setting2").value("value2").build());

    List<UserSettingConfiguration> defaultUserSettings = Arrays.asList(UserSettingConfiguration.builder()
                                                                           .identifier("setting1")
                                                                           .valueType(SettingValueType.STRING)
                                                                           .defaultValue("default1")
                                                                           .build(),
        UserSettingConfiguration.builder()
            .identifier("setting2")
            .valueType(SettingValueType.BOOLEAN)
            .defaultValue("false")
            .build());

    ArgumentCaptor<UserSetting> userSettingCaptor = ArgumentCaptor.forClass(UserSetting.class);
    ArgumentCaptor<UserSettingConfiguration> userSettingConfigurationCaptor =
        ArgumentCaptor.forClass(UserSettingConfiguration.class);

    when(userSettingConfigurationRepository.findAll(any())).thenReturn(defaultUserSettings);
    when(userSettingRepository.listUserSettingForAccount(anyString(), anyString(), any(), any()))
        .thenReturn(userSettingForAccount);
    when(userSettingMapper.writeUserSettingResponseDTO(
             userSettingCaptor.capture(), userSettingConfigurationCaptor.capture()))
        .thenReturn(mock(UserSettingResponseDTO.class));

    userSettingsService.list(accountIdentifier, userId, null, null);

    verify(userSettingConfigurationRepository, times(2)).findAll(any());
    verify(userSettingRepository, times(1)).listUserSettingForAccount(accountIdentifier, userId, null, null);
    verify(userSettingMapper, times(2))
        .writeUserSettingResponseDTO(any(UserSetting.class), any(UserSettingConfiguration.class));

    UserSetting capturedUserSetting = userSettingCaptor.getValue();
    UserSettingConfiguration capturedUserSettingConfiguration = userSettingConfigurationCaptor.getValue();

    assertThat(capturedUserSetting).isNotNull();
    assertThat(capturedUserSetting.getIdentifier()).isIn("setting1", "setting2");
    assertThat(capturedUserSetting.getValue()).isIn("value1", "value2");
    assertThat(capturedUserSettingConfiguration).isNotNull();
    assertThat(capturedUserSettingConfiguration.getIdentifier()).isIn("setting1", "setting2");
    assertThat(capturedUserSettingConfiguration.getValueType()).isIn(SettingValueType.STRING, SettingValueType.BOOLEAN);
    assertThat(capturedUserSettingConfiguration.getDefaultValue()).isIn("default1", "false");
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testUpdateWithTypeRestore() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);

    UserSettingRequestDTO userSettingRequestDTO = UserSettingRequestDTO.builder().build();
    String validIdentifier = "validIdentifier";
    userSettingRequestDTO.setIdentifier(validIdentifier);
    userSettingRequestDTO.setValue("true");
    userSettingRequestDTO.setUpdateType(SettingUpdateType.RESTORE);

    UserSettingConfiguration defaultUserSetting = new UserSettingConfiguration();
    defaultUserSetting.setIdentifier(validIdentifier);
    defaultUserSetting.setValueType(SettingValueType.BOOLEAN);
    defaultUserSetting.setDefaultValue("false");

    UserSetting currentUserSetting = UserSetting.builder().build();
    currentUserSetting.setIdentifier(validIdentifier);
    currentUserSetting.setValue("true");
    currentUserSetting.setUserId(userId);
    currentUserSetting.setValueType(SettingValueType.BOOLEAN);

    UserSettingDTO userSettingDTO =
        UserSettingDTO.builder().userID(userId).valueType(SettingValueType.BOOLEAN).identifier(validIdentifier).build();

    UserSettingUpdateResponseDTO userSettingUpdateResponseDTO = UserSettingUpdateResponseDTO.builder()
                                                                    .userSettingDTO(userSettingDTO)
                                                                    .identifier(validIdentifier)
                                                                    .updateStatus(true)
                                                                    .build();

    when(userSettingConfigurationRepository.findByIdentifier(validIdentifier)).thenReturn(defaultUserSetting);

    when(userSettingRepository.getUserSettingByIdentifier(any(), any(), any())).thenReturn(currentUserSetting);
    when(userSettingConfigurationRepository.findAll(any())).thenReturn(Collections.singletonList(defaultUserSetting));
    when(userSettingMapper.writeUserSettingUpdateDTO(any(), any())).thenReturn(userSettingUpdateResponseDTO);
    List<UserSettingUpdateResponseDTO> result =
        userSettingsService.update(accountIdentifier, userId, Collections.singletonList(userSettingRequestDTO));

    verify(userSettingRepository, times(1)).getUserSettingByIdentifier(any(), any(), any());
    verify(userSettingRepository, times(1)).save(any(UserSetting.class));

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getIdentifier()).isEqualTo(validIdentifier);
    assertThat(result.get(0).getUserSettingDTO().getIdentifier()).isEqualTo(validIdentifier);
    assertThat(result.get(0).getUserSettingDTO().getUserID()).isEqualTo(userId);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testUpdateWithTypeUpdate() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);

    UserSettingRequestDTO userSettingRequestDTO = UserSettingRequestDTO.builder().build();
    String validIdentifier = "validIdentifier";
    userSettingRequestDTO.setIdentifier(validIdentifier);
    userSettingRequestDTO.setValue("true");
    userSettingRequestDTO.setUpdateType(SettingUpdateType.UPDATE);

    UserSettingConfiguration defaultUserSetting = new UserSettingConfiguration();
    defaultUserSetting.setIdentifier(validIdentifier);
    defaultUserSetting.setValueType(SettingValueType.BOOLEAN);
    defaultUserSetting.setDefaultValue("false");

    UserSetting currentUserSetting = UserSetting.builder().build();
    currentUserSetting.setIdentifier(validIdentifier);
    currentUserSetting.setValue("false");
    currentUserSetting.setUserId(userId);
    currentUserSetting.setValueType(SettingValueType.BOOLEAN);

    UserSettingDTO userSettingDTO =
        UserSettingDTO.builder().userID(userId).valueType(SettingValueType.BOOLEAN).identifier(validIdentifier).build();

    UserSettingUpdateResponseDTO userSettingUpdateResponseDTO = UserSettingUpdateResponseDTO.builder()
                                                                    .userSettingDTO(userSettingDTO)
                                                                    .identifier(validIdentifier)
                                                                    .updateStatus(true)
                                                                    .build();

    when(userSettingConfigurationRepository.findByIdentifier(validIdentifier)).thenReturn(defaultUserSetting);

    when(userSettingRepository.getUserSettingByIdentifier(any(), any(), any())).thenReturn(currentUserSetting);
    when(userSettingConfigurationRepository.findAll(any())).thenReturn(Collections.singletonList(defaultUserSetting));
    when(userSettingMapper.writeUserSettingUpdateDTO(any(), any())).thenReturn(userSettingUpdateResponseDTO);
    List<UserSettingUpdateResponseDTO> result =
        userSettingsService.update(accountIdentifier, userId, Collections.singletonList(userSettingRequestDTO));

    verify(userSettingRepository, times(1)).getUserSettingByIdentifier(any(), any(), any());
    verify(userSettingRepository, times(1)).save(any(UserSetting.class));

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getIdentifier()).isEqualTo(validIdentifier);
    assertThat(result.get(0).getUserSettingDTO().getIdentifier()).isEqualTo(validIdentifier);
    assertThat(result.get(0).getUserSettingDTO().getUserID()).isEqualTo(userId);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void testGetUserPreferences() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);

    List<UserSetting> userSettingForAccount =
        Arrays.asList(UserSetting.builder().identifier("setting1").value("value1").build(),
            UserSetting.builder().identifier("setting2").value("value2").build());

    when(userSettingRepository.listUserSettingForAccount(anyString(), anyString(), any(), any()))
        .thenReturn(userSettingForAccount);
    when(userSettingConfigurationRepository.findByIdentifier(anyString())).thenReturn(null);
    Map<String, String> result = userSettingsService.getUserPreferences(accountIdentifier, userId);

    verify(userSettingRepository, times(1)).listUserSettingForAccount(accountIdentifier, userId, null, null);
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result).containsEntry("setting1", "value1");
    assertThat(result).containsEntry("setting2", "value2");
  }
}
