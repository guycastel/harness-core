/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.ngtriggers.beans.source.v1.YamlSimplConstants.AMAZON_S3;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Value;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDC)
@Value
public class AmazonS3RegistrySpec implements ArtifactTypeSpec {
  String connector;
  Conditions conditions;
  String region;
  String bucket;
  String path_regex;

  @Override
  public String fetchConnectorRef() {
    return connector;
  }

  @Override
  public String fetchBuildType() {
    return AMAZON_S3;
  }

  @Override
  public List<TriggerEventDataCondition> fetchEventDataConditions() {
    return conditions.getEvent();
  }

  @Override
  public List<TriggerEventDataCondition> fetchMetaDataConditions() {
    return conditions.getMetadata();
  }

  @Override
  public String fetchJexlArtifactConditions() {
    return conditions.getJexl();
  }
}
