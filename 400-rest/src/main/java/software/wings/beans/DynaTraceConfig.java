/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictions;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;
import software.wings.sm.states.APMVerificationState;
import software.wings.yaml.setting.VerificationProviderYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by raghu on 8/28/17.
 */
@JsonTypeName("DYNA_TRACE")
@Data
@Builder
@ToString(exclude = "apiToken")
@EqualsAndHashCode(callSuper = false)
public class DynaTraceConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "URL", required = true) private String dynaTraceUrl;

  @Attributes(title = "API Token", required = true) @Encrypted(fieldName = "api_token") private char[] apiToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApiToken;

  public DynaTraceConfig() {
    super(StateType.DYNA_TRACE.name());
  }

  public DynaTraceConfig(String dynaTraceUrl, char[] apiToken, String accountId, String encryptedApiToken) {
    this();
    this.dynaTraceUrl = dynaTraceUrl;
    this.apiToken = apiToken == null ? null : apiToken.clone();
    this.accountId = accountId;
    this.encryptedApiToken = encryptedApiToken;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        dynaTraceUrl, maskingEvaluator));
  }

  private Map<String, String> headerMap() {
    Map<String, String> headerMap = new HashMap<>();
    // check for apiKey. If not empty populate the value else populate default value.
    headerMap.put("Authorization", apiToken != null ? "Api-Token " + new String(apiToken) : "Api-Token ${apiToken}");
    headerMap.put("Accept", "application/json");
    return headerMap;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig(SecretManager secretManager) {
    return APMValidateCollectorConfig.builder()
        .baseUrl(dynaTraceUrl)
        .collectionMethod(APMVerificationState.Method.GET)
        .headers(headerMap())
        .encryptedDataDetails(secretManager.getEncryptionDetails(this))
        .build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class DynaTraceYaml extends VerificationProviderYaml {
    private String apiToken;
    private String dynaTraceUrl;

    @Builder
    public DynaTraceYaml(String type, String harnessApiVersion, String dynaTraceUrl, String apiToken,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.dynaTraceUrl = dynaTraceUrl;
      this.apiToken = apiToken;
    }
  }
}
