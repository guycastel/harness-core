/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.bamboo;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.bamboo.dtos.BambooPlanKeysDTO;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface BambooResourceService {
  BambooPlanKeysDTO getPlanName(IdentifierRef bambooConnectorRef, String orgIdentifier, String projectIdentifier);
  List<String> getArtifactPath(
      IdentifierRef bambooConnectorRef, String orgIdentifier, String projectIdentifier, String planName);
  List<BuildDetails> getBuilds(IdentifierRef bambooConnectorRef, String orgIdentifier, String projectIdentifier,
      String planName, List<String> artifactPath);
}
