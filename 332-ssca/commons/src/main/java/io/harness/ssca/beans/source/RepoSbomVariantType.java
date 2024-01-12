/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RepoSbomVariantType {
  @JsonProperty(SbomSourceConstants.BRANCH) BRANCH(SbomSourceConstants.BRANCH),
  @JsonProperty(SbomSourceConstants.GIT_TAG) GIT_TAG(SbomSourceConstants.GIT_TAG),
  @JsonProperty(SbomSourceConstants.COMMIT) COMMIT(SbomSourceConstants.COMMIT);
  private String name;

  RepoSbomVariantType(String name) {
    this.name = name;
  }

  @Override
  @JsonValue
  public String toString() {
    return this.name;
  }
}
