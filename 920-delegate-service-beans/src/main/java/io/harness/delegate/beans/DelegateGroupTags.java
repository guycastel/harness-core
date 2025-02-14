/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.DEL)
@Value
public final class DelegateGroupTags {
  private final Set<String> tags;

  public Set<String> getTags() {
    return tags.stream().filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toSet());
  }
}
