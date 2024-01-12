/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers.overrides;

import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_GLOBAL_OVERRIDE;
import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_SERVICE_OVERRIDE;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.APPLICATION_SETTINGS;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.CONFIG_FILES;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.CONNECTION_STRINGS;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.COUNT;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.ENVIRONMENT_REF;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.INFRA_IDENTIFIER;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.MANIFEST_OVERRIDE;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.OVERRIDE_SOURCES;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.OVERRIDE_TYPES;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.OVERRIDE_V2;
import static io.harness.telemetry.helpers.overrides.OverrideInstrumentConstants.VARIABLE_OVERRIDE;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.azure.config.yaml.ApplicationSettingsConfiguration;
import io.harness.cdng.azure.config.yaml.ConnectionStringsConfiguration;
import io.harness.cdng.configfile.ConfigFileWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.ng.core.environment.beans.NGEnvironmentGlobalOverride;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideInfoConfig;
import io.harness.ng.core.serviceoverridev2.beans.NGServiceOverrideConfigV2;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesSpec;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class OverrideInstrumentationHelper {
  public HashMap<String, Object> updateTelemetryMapForOverrideV2(
      EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs) {
    try {
      HashMap<String, Object> eventPropertiesMap = new HashMap<>();
      Set<String> overrideType = new HashSet<>();
      Set<String> overrideSource = new HashSet<>();
      List<NGServiceOverrideConfigV2> ngServiceOverrideConfigV2List = new ArrayList<>(mergedOverrideV2Configs.values());
      ngServiceOverrideConfigV2List.forEach(ngServiceOverrideConfigV2
          -> updateOverrideV2Event(ngServiceOverrideConfigV2, eventPropertiesMap, overrideType, overrideSource));
      eventPropertiesMap.put(OVERRIDE_TYPES, overrideType);
      eventPropertiesMap.put(OVERRIDE_SOURCES, overrideSource);
      return eventPropertiesMap;
    } catch (Exception e) {
      log.error("Failed to update the override v2 telemetry data", e);
    }
    return new HashMap<>();
  }

  public HashMap<String, Object> updateTelemetryMapForOverrideV1(
      NGServiceOverrideConfig ngServiceOverrides, NGEnvironmentConfig ngEnvironmentConfig) {
    try {
      Set<String> overrideType = new HashSet<>();
      Set<String> overrideSource = new HashSet<>();
      HashMap<String, Object> eventPropertiesMap = new HashMap<>();
      if (ngServiceOverrides != null && ngServiceOverrides.getServiceOverrideInfoConfig() != null) {
        eventPropertiesMap.put(OVERRIDE_V2, false);
        eventPropertiesMap.put(ENVIRONMENT_REF, ngServiceOverrides.getServiceOverrideInfoConfig().getEnvironmentRef());
        updateOverrideV1EventForServiceAndEnvOverrides(ngServiceOverrides.getServiceOverrideInfoConfig(),
            ngEnvironmentConfig, eventPropertiesMap, overrideType, overrideSource);
        eventPropertiesMap.put(OVERRIDE_TYPES, overrideType);
        eventPropertiesMap.put(OVERRIDE_SOURCES, overrideSource);
        return eventPropertiesMap;
      }
    } catch (Exception e) {
      log.error("Failed to update the override v1 telemetry data", e);
    }
    return new HashMap<>();
  }

  public void updateTelemetryMapForOverrideV1MultipleEnvs(NGServiceOverrideConfig ngServiceOverrides,
      NGEnvironmentConfig ngEnvironmentConfig, HashMap<String, Object> eventPropertiesMap, String envRef) {
    try {
      Set<String> overrideType = new HashSet<>();
      Set<String> overrideSource = new HashSet<>();
      if (ngServiceOverrides != null && ngServiceOverrides.getServiceOverrideInfoConfig() != null) {
        eventPropertiesMap.putIfAbsent(OVERRIDE_V2, false);
        updateOverrideV1EventForServiceAndEnvOverrides(ngServiceOverrides.getServiceOverrideInfoConfig(),
            ngEnvironmentConfig, eventPropertiesMap, overrideType, overrideSource);
        ((List<String>) eventPropertiesMap.computeIfAbsent(ENVIRONMENT_REF, key -> new ArrayList<>())).add(envRef);
        ((HashSet<String>) eventPropertiesMap.computeIfAbsent(OVERRIDE_TYPES, key -> new HashSet<>()))
            .addAll(overrideType);
        ((HashSet<String>) eventPropertiesMap.computeIfAbsent(OVERRIDE_SOURCES, key -> new HashSet<>()))
            .addAll(overrideSource);
      }
    } catch (Exception e) {
      log.error("Failed to update the override v1 telemetry data", e);
    }
  }

  private void updateOverrideV1EventForServiceAndEnvOverrides(NGServiceOverrideInfoConfig ngServiceOverrideInfoConfig,
      NGEnvironmentConfig ngEnvironmentInfoConfig, HashMap<String, Object> eventPropertiesMap, Set<String> overrideType,
      Set<String> overrideSource) {
    // for service overrides
    increaseCountInPropertyMap(eventPropertiesMap, ENV_SERVICE_OVERRIDE.toString());
    overrideType.add(ENV_SERVICE_OVERRIDE.toString());
    addManifestCountToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getManifests(), overrideSource);
    addVariableCountToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getVariables(), overrideSource);
    addApplicationSettingToMap(
        eventPropertiesMap, ngServiceOverrideInfoConfig.getApplicationSettings(), overrideSource);
    addConfigFilesToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getConfigFiles(), overrideSource);
    addConnectionStringsConfigToMap(
        eventPropertiesMap, ngServiceOverrideInfoConfig.getConnectionStrings(), overrideSource);

    // for environment overrides
    if (ngEnvironmentInfoConfig != null && ngEnvironmentInfoConfig.getNgEnvironmentInfoConfig() != null
        && ngEnvironmentInfoConfig.getNgEnvironmentInfoConfig().getNgEnvironmentGlobalOverride() != null) {
      NGEnvironmentGlobalOverride ngEnvironmentGlobalOverride =
          ngEnvironmentInfoConfig.getNgEnvironmentInfoConfig().getNgEnvironmentGlobalOverride();

      increaseCountInPropertyMap(eventPropertiesMap, ENV_GLOBAL_OVERRIDE.toString());
      overrideType.add(ENV_GLOBAL_OVERRIDE.toString());
      addManifestCountToMap(eventPropertiesMap, ngEnvironmentGlobalOverride.getManifests(), overrideSource);
      addVariableCountToMap(
          eventPropertiesMap, ngEnvironmentInfoConfig.getNgEnvironmentInfoConfig().getVariables(), overrideSource);
      addApplicationSettingToMap(
          eventPropertiesMap, ngEnvironmentGlobalOverride.getApplicationSettings(), overrideSource);
      addConfigFilesToMap(eventPropertiesMap, ngEnvironmentGlobalOverride.getConfigFiles(), overrideSource);
      addConnectionStringsConfigToMap(
          eventPropertiesMap, ngEnvironmentGlobalOverride.getConnectionStrings(), overrideSource);
    }
  }

  private void updateOverrideV2Event(NGServiceOverrideConfigV2 ngServiceOverrideConfigV2,
      HashMap<String, Object> eventPropertiesMap, Set<String> overrideType, Set<String> overrideSource) {
    if (ngServiceOverrideConfigV2 != null) {
      ServiceOverridesSpec spec = ngServiceOverrideConfigV2.getSpec();

      // We are sending Global env override object with all properties as  null even if no override v2 is there. This
      // check prevents this null override event to telemetry
      if (isNull(spec.getVariables()) && isNull(spec.getApplicationSettings()) && isNull(spec.getManifests())
          && isNull(spec.getConnectionStrings()) && isNull(spec.getConfigFiles())) {
        return;
      }

      String overrideTypeString = ngServiceOverrideConfigV2.getType().toString();
      overrideType.add(overrideTypeString);
      increaseCountInPropertyMap(eventPropertiesMap, overrideTypeString);

      eventPropertiesMap.put(OVERRIDE_V2, true);
      eventPropertiesMap.put(ENVIRONMENT_REF, ngServiceOverrideConfigV2.getEnvironmentRef());
      eventPropertiesMap.put(INFRA_IDENTIFIER, ngServiceOverrideConfigV2.getInfraId());

      addManifestCountToMap(eventPropertiesMap, spec.getManifests(), overrideSource);
      addVariableCountToMap(eventPropertiesMap, spec.getVariables(), overrideSource);
      addApplicationSettingToMap(eventPropertiesMap, spec.getApplicationSettings(), overrideSource);
      addConfigFilesToMap(eventPropertiesMap, spec.getConfigFiles(), overrideSource);
      addConnectionStringsConfigToMap(eventPropertiesMap, spec.getConnectionStrings(), overrideSource);
    }
  }

  private void addConfigFilesToMap(
      HashMap<String, Object> eventPropertiesMap, List<ConfigFileWrapper> configFiles, Set<String> overrideSource) {
    if (configFiles != null) {
      eventPropertiesMap.put(
          CONFIG_FILES, (Integer) eventPropertiesMap.getOrDefault(CONFIG_FILES + COUNT, 0) + configFiles.size());
      overrideSource.add(CONFIG_FILES);
    }
  }

  private void addConnectionStringsConfigToMap(HashMap<String, Object> eventPropertiesMap,
      ConnectionStringsConfiguration connectionStrings, Set<String> overrideSource) {
    if (connectionStrings != null) {
      increaseCountInPropertyMap(eventPropertiesMap, CONNECTION_STRINGS + COUNT);
      overrideSource.add(CONNECTION_STRINGS);
    }
  }

  private void addApplicationSettingToMap(HashMap<String, Object> eventPropertiesMap,
      ApplicationSettingsConfiguration applicationSettings, Set<String> overrideSource) {
    if (applicationSettings != null) {
      increaseCountInPropertyMap(eventPropertiesMap, APPLICATION_SETTINGS + COUNT);
      overrideSource.add(APPLICATION_SETTINGS);
    }
  }

  private void addVariableCountToMap(
      HashMap<String, Object> eventPropertiesMap, List<NGVariable> variables, Set<String> overrideSource) {
    if (variables != null) {
      variables.forEach(variable -> {
        String variableKey = variable.getType().toString().toLowerCase(Locale.ROOT) + "_" + VARIABLE_OVERRIDE;
        increaseCountInPropertyMap(eventPropertiesMap, variableKey + COUNT);
        overrideSource.add(variableKey);
      });
    }
  }

  private void addManifestCountToMap(HashMap<String, Object> eventPropertiesMap,
      List<ManifestConfigWrapper> manifestConfigWrappers, Set<String> overrideSource) {
    if (manifestConfigWrappers != null) {
      manifestConfigWrappers.forEach(manifestConfigWrapper -> {
        if (manifestConfigWrapper.getManifest() != null) {
          String manifestKey = manifestConfigWrapper.getManifest().getType().toString().toLowerCase(Locale.ROOT) + "_"
              + MANIFEST_OVERRIDE;
          increaseCountInPropertyMap(eventPropertiesMap, manifestKey + COUNT);
          overrideSource.add(manifestKey);
        }
      });
    }
  }

  private void increaseCountInPropertyMap(HashMap<String, Object> eventPropertiesMap, String key) {
    eventPropertiesMap.put(key, (Integer) eventPropertiesMap.getOrDefault(key, 0) + 1);
  }
}
