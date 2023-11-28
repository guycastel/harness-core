/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapoints.parser.scm.github;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.common.CommonUtils;
import io.harness.idp.scorecard.datapoints.parser.scm.ScmFileContainsParser;

import java.util.Map;

@OwnedBy(HarnessTeam.IDP)
public class GithubFileContainsParser extends ScmFileContainsParser {
  @Override
  protected String getFileContent(Map<String, Object> data) {
    if (CommonUtils.findObjectByName(data, "object") == null) {
      return null;
    }
    return (String) CommonUtils.findObjectByName(data, "text");
  }
}
