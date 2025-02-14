/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.docker.service;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.NestedExceptionUtils;

import lombok.experimental.UtilityClass;
import retrofit2.Response;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ARTIFACTS})
@UtilityClass
public class ArtifactUtils {
  private static final String RESPONSE_NULL = "Response Is Null";
  public boolean checkIfResponseNull(Response<?> response) {
    if (response == null) {
      throw NestedExceptionUtils.hintWithExplanationException(RESPONSE_NULL,
          "Please Check Whether Artifact exists or not", new InvalidArtifactServerException(RESPONSE_NULL, USER));
    }
    return false;
  }

  public boolean isSHA(String s) {
    if (s.indexOf(':') == -1) {
      return false;
    }
    return true;
  }

  public String getImageName(String image, String tag) {
    if (isSHA(tag)) {
      return String.format("%s@%s", image, tag);
    }
    return String.format("%s:%s", image, tag);
  }
}
