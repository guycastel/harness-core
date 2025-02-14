/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ngsettings.SettingConstants.GLOBAL_ACCOUNT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingUpdateType;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.SettingConfiguration.SettingConfigurationKeys;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.mapper.UserSettingMapper;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.repositories.ngsettings.spring.SettingRepository;
import io.harness.repositories.ngsettings.spring.UserSettingConfigurationRepository;
import io.harness.repositories.ngsettings.spring.UserSettingRepository;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)

public class UserSettingsServiceImpl implements UserSettingsService {
  private final UserSettingConfigurationRepository userSettingConfigurationRepository;
  private final SettingRepository settingRepository;

  private final UserSettingRepository userSettingRepository;

  private final UserSettingMapper userSettingMapper;

  @Override
  public SettingValueResponseDTO get(String identifier, String accountIdentifier, String userIdentifier) {
    Set<String> defaultUserSettingIdentifiers =
        listDefaultSettings().stream().map(UserSettingConfiguration::getIdentifier).collect(Collectors.toSet());
    if (!defaultUserSettingIdentifiers.contains(identifier)) {
      throw new InvalidRequestException("No user setting found for this identifier");
    }

    UserSetting userSettingForAccount = getUserSettingValueForAnAccount(identifier, accountIdentifier, userIdentifier);
    if (userSettingForAccount != null) {
      return SettingValueResponseDTO.builder()
          .valueType(userSettingForAccount.getValueType())
          .value(userSettingForAccount.getValue())
          .build();
    }
    UserSetting userSettingAcrossAccounts = getUserSettingValueAcrossAccounts(identifier, userIdentifier);
    if (userSettingAcrossAccounts != null) {
      return SettingValueResponseDTO.builder()
          .valueType(userSettingAcrossAccounts.getValueType())
          .value(userSettingAcrossAccounts.getValue())
          .build();
    }
    UserSettingConfiguration defaultUserSetting = userSettingConfigurationRepository.findByIdentifier(identifier);

    return SettingValueResponseDTO.builder()
        .valueType(defaultUserSetting.getValueType())
        .value(defaultUserSetting.getDefaultValue())
        .build();
  }

  @Override
  public List<UserSettingConfiguration> listDefaultSettings() {
    List<UserSettingConfiguration> userSettingConfigurations = new ArrayList<>();
    Criteria criteria = Criteria.where(SettingConfigurationKeys.category).is(SettingCategory.USER);
    for (UserSettingConfiguration userSettingConfiguration : userSettingConfigurationRepository.findAll(criteria)) {
      userSettingConfigurations.add(userSettingConfiguration);
    }

    return userSettingConfigurations;
  }

  @Override
  public UserSettingConfiguration upsertSettingConfiguration(UserSettingConfiguration userSettingConfiguration) {
    return userSettingConfigurationRepository.save(userSettingConfiguration);
  }

  @Override
  public void removeSetting(String identifier) {
    UserSettingConfiguration existingUserSettingConfig =
        userSettingConfigurationRepository.findByIdentifier(identifier);
    if (existingUserSettingConfig != null) {
      userSettingConfigurationRepository.delete(existingUserSettingConfig);
      log.info("Deleted the user setting configuration from the database- {}", existingUserSettingConfig);
    }
    List<Setting> existingUserSetting = settingRepository.findByIdentifier(identifier);
    settingRepository.deleteAll(existingUserSetting);
  }

  @Override
  public List<UserSettingResponseDTO> list(
      String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
    List<UserSettingResponseDTO> userSettingResponseDTOS = new ArrayList<>();
    List<UserSetting> userSettingForAccount =
        listUserSettingForAccount(accountIdentifier, userIdentifier, category, groupIdentifier);
    List<UserSettingConfiguration> defaultUserSettings = listDefaultSettings();
    defaultUserSettings.stream()
        .filter(defaultUserSetting
            -> userSettingForAccount.stream().anyMatch(
                accountSetting -> accountSetting.getIdentifier().equals(defaultUserSetting.getIdentifier())))
        .map(defaultUserSetting -> {
          UserSetting accountSetting =
              userSettingForAccount.stream()
                  .filter(account -> account.getIdentifier().equals(defaultUserSetting.getIdentifier()))
                  .findFirst()
                  .orElse(null);
          if (accountSetting != null) {
            return userSettingMapper.writeUserSettingResponseDTO(accountSetting, defaultUserSetting);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .forEach(userSettingResponseDTOS::add);
    return userSettingResponseDTOS;
  }

  @Override
  public List<UserSettingUpdateResponseDTO> update(
      String accountIdentifier, String userIdentifier, List<UserSettingRequestDTO> userSettingRequestDTOList) {
    List<UserSettingConfiguration> defaultUserSettings = listDefaultSettings();
    Set<String> defaultUserSettingIdentifiers =
        defaultUserSettings.stream().map(UserSettingConfiguration::getIdentifier).collect(Collectors.toSet());
    List<UserSettingUpdateResponseDTO> userSettingUpdateResponseDTOS = new ArrayList<>();
    userSettingRequestDTOList.forEach(userSettingRequestDTO -> {
      String accountId =
          Boolean.TRUE.equals(userSettingRequestDTO.getEnableAcrossAccounts()) ? GLOBAL_ACCOUNT : accountIdentifier;
      if (!defaultUserSettingIdentifiers.contains(userSettingRequestDTO.getIdentifier())) {
        throw new InvalidRequestException(
            "Error: Invalid User setting identifier: " + userSettingRequestDTO.getIdentifier());
      }
      if (userSettingRequestDTO.getUpdateType() == SettingUpdateType.UPDATE) {
        userSettingUpdateResponseDTOS.add(upsertUserSetting(accountId, userIdentifier, userSettingRequestDTO));
      } else {
        userSettingUpdateResponseDTOS.add(restoreUserSetting(accountId, userIdentifier, userSettingRequestDTO));
      }
    });

    return userSettingUpdateResponseDTOS;
  }

  @Override
  public Map<String, String> getUserPreferences(String accountIdentifier, String userIdentifier) {
    List<UserSetting> userSettings = listUserSettingForAccount(accountIdentifier, userIdentifier, null, null);
    return userSettings.stream().collect(Collectors.toMap(UserSetting::getIdentifier, UserSetting::getValue));
  }

  private List<UserSetting> listUserSettingForAccount(
      String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
    List<UserSettingConfiguration> defaultUserSettings = listDefaultSettings();

    List<UserSetting> userSettingForAccount =
        userSettingRepository.listUserSettingForAccount(accountIdentifier, userIdentifier, category, groupIdentifier);
    if (!checkIfAllUserSettingPresent(userSettingForAccount, defaultUserSettings)) {
      List<UserSetting> userSettingForGlobalAccount =
          userSettingRepository.listUserSettingForAccount(GLOBAL_ACCOUNT, userIdentifier, category, groupIdentifier);
      userSettingForGlobalAccount.stream()
          .filter(globalSetting
              -> !userSettingForAccount.stream().anyMatch(
                  accountSetting -> accountSetting.getIdentifier().equals(globalSetting.getIdentifier())))
          .forEach(userSettingForAccount::add);
    }
    if (!checkIfAllUserSettingPresent(userSettingForAccount, defaultUserSettings)) {
      Set<String> userSettingIdentifiers =
          userSettingForAccount.stream().map(UserSetting::getIdentifier).collect(Collectors.toSet());
      defaultUserSettings.stream()
          .filter(defaultUserSetting -> !userSettingIdentifiers.contains(defaultUserSetting.getIdentifier()))
          .map(defaultUserSetting
              -> userSettingMapper.toUserSetting(accountIdentifier, userIdentifier, defaultUserSetting))
          .forEach(userSettingForAccount::add);
    }

    return userSettingForAccount;
  }

  private UserSetting getUserSettingValueForAnAccount(
      String identifier, String accountIdentifier, String userIdentifier) {
    return userSettingRepository.getUserSettingByIdentifier(accountIdentifier, userIdentifier, identifier);
  }

  private UserSetting getUserSettingValueAcrossAccounts(String identifier, String userIdentifier) {
    return userSettingRepository.getUserSettingByIdentifier(GLOBAL_ACCOUNT, userIdentifier, identifier);
  }

  private boolean checkIfAllUserSettingPresent(
      List<UserSetting> userSettingForAccount, List<UserSettingConfiguration> defaultUserSettings) {
    return userSettingForAccount.size() == defaultUserSettings.size();
  }

  private UserSettingUpdateResponseDTO upsertUserSetting(
      String accountIdentifier, String userIdentifier, UserSettingRequestDTO userSettingRequestDTO) {
    UserSettingConfiguration defaultUserSetting =
        userSettingConfigurationRepository.findByIdentifier(userSettingRequestDTO.getIdentifier());

    UserSetting currentUserSetting =
        getUserSettingValueForAnAccount(userSettingRequestDTO.getIdentifier(), accountIdentifier, userIdentifier);
    if (currentUserSetting != null) {
      currentUserSetting.setValue(userSettingRequestDTO.getValue());
      userSettingRepository.save(currentUserSetting);
      return userSettingMapper.writeUserSettingUpdateDTO(currentUserSetting, defaultUserSetting);
    }
    UserSetting userSetting = userSettingMapper.toUserSetting(accountIdentifier, userIdentifier, defaultUserSetting);
    userSetting.setValue(userSettingRequestDTO.getValue());
    userSettingRepository.save(userSetting);
    return userSettingMapper.writeUserSettingUpdateDTO(userSetting, defaultUserSetting);
  }

  private UserSettingUpdateResponseDTO restoreUserSetting(
      String accountIdentifier, String userIdentifier, UserSettingRequestDTO userSettingRequestDTO) {
    UserSettingConfiguration defaultUserSetting =
        userSettingConfigurationRepository.findByIdentifier(userSettingRequestDTO.getIdentifier());

    UserSetting currentUserSetting =
        getUserSettingValueForAnAccount(userSettingRequestDTO.getIdentifier(), accountIdentifier, userIdentifier);
    if (currentUserSetting != null) {
      currentUserSetting.setValue(defaultUserSetting.getDefaultValue());
      userSettingRepository.save(currentUserSetting);
      return userSettingMapper.writeUserSettingUpdateDTO(currentUserSetting, defaultUserSetting);
    }
    UserSetting userSetting = userSettingMapper.toUserSetting(accountIdentifier, userIdentifier, defaultUserSetting);
    userSettingRepository.save(userSetting);
    return userSettingMapper.writeUserSettingUpdateDTO(userSetting, defaultUserSetting);
  }
}
