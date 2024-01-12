/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.impl;

import static io.harness.beans.ScopeLevel.ACCOUNT;
import static io.harness.beans.ScopeLevel.ORGANIZATION;
import static io.harness.beans.ScopeLevel.PROJECT;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;

public class ScopeInfoHelper {
  private static final String SCOPE_INFO_CACHE_KEY_DELIMITER = "/";
  ScopeInfo populateScopeInfo(final ScopeLevel scopeType, final String uniqueId, final String accountIdentifier,
      final String orgIdentifier, final String projIdentifier) {
    ScopeInfo builtScope =
        ScopeInfo.builder().scopeType(scopeType).uniqueId(uniqueId).accountIdentifier(accountIdentifier).build();
    if (ORGANIZATION.equals(scopeType)) {
      builtScope.setOrgIdentifier(orgIdentifier);
    } else if (PROJECT.equals(scopeType)) {
      builtScope.setOrgIdentifier(orgIdentifier);
      builtScope.setProjectIdentifier(projIdentifier);
    }
    return builtScope;
  }

  String getScopeInfoCacheKey(
      final String accountIdentifier, final String orgIdentifier, final String projectIdentifier) {
    // key-format: ACCOUNT/<accountIdentifier>/ORGANIZATION/orgIdentifier/PROJECT/projectIdentifier
    // append ACCOUNT
    StringBuilder sb =
        new StringBuilder().append(ACCOUNT.name()).append(SCOPE_INFO_CACHE_KEY_DELIMITER).append(accountIdentifier);
    // append ORG
    if (isNotEmpty(orgIdentifier)) {
      sb.append(SCOPE_INFO_CACHE_KEY_DELIMITER)
          .append(ScopeLevel.ORGANIZATION.name())
          .append(SCOPE_INFO_CACHE_KEY_DELIMITER)
          .append(orgIdentifier);
    }
    // append PROJECT
    if (isNotEmpty(orgIdentifier) && isNotEmpty(projectIdentifier)) {
      sb.append(SCOPE_INFO_CACHE_KEY_DELIMITER)
          .append(ScopeLevel.PROJECT.name())
          .append(SCOPE_INFO_CACHE_KEY_DELIMITER)
          .append(projectIdentifier);
    }
    return sb.toString();
  }

  String getScopeInfoUniqueIdCacheKey(final String uniqueId) {
    return uniqueId;
  }
}
