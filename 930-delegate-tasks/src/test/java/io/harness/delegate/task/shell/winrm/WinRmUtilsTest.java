/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell.winrm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.VITALIE;

import static software.wings.common.Constants.WINDOWS_HOME_DIR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.ssh.CopyCommandUnit;
import io.harness.delegate.task.ssh.NgCleanupCommandUnit;
import io.harness.delegate.task.ssh.NgDownloadArtifactCommandUnit;
import io.harness.delegate.task.ssh.NgInitCommandUnit;
import io.harness.delegate.task.ssh.ScriptCommandUnit;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(CDP)
@RunWith(MockitoJUnitRunner.class)
public class WinRmUtilsTest {
  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testGetWorkingDir() {
    final String workingDirectory = "C:\\workDir";
    final String destinationPath = "C:\\destinationPath";

    ScriptCommandUnit scriptCommandUnit = ScriptCommandUnit.builder().workingDirectory(workingDirectory).build();
    String ret = WinRmUtils.getWorkingDir(scriptCommandUnit, true);
    assertThat(ret).isEqualTo(workingDirectory);

    ret = WinRmUtils.getWorkingDir(scriptCommandUnit, false);
    assertThat(ret).isEqualTo(WINDOWS_HOME_DIR);

    CopyCommandUnit copyCommandUnit = CopyCommandUnit.builder().destinationPath(destinationPath).build();
    ret = WinRmUtils.getWorkingDir(copyCommandUnit, true);
    assertThat(ret).isEqualTo(destinationPath);

    ret = WinRmUtils.getWorkingDir(copyCommandUnit, false);
    assertThat(ret).isEqualTo(destinationPath);

    NgDownloadArtifactCommandUnit ngDownloadArtifactCommandUnit =
        NgDownloadArtifactCommandUnit.builder().destinationPath(destinationPath).build();
    ret = WinRmUtils.getWorkingDir(ngDownloadArtifactCommandUnit, true);
    assertThat(ret).isEqualTo(destinationPath);

    ret = WinRmUtils.getWorkingDir(ngDownloadArtifactCommandUnit, false);
    assertThat(ret).isEqualTo(destinationPath);

    NgInitCommandUnit ngInitCommandUnit = NgInitCommandUnit.builder().build();
    ret = WinRmUtils.getWorkingDir(ngInitCommandUnit, true);
    assertThat(ret).isEqualTo(WINDOWS_HOME_DIR);

    ret = WinRmUtils.getWorkingDir(ngInitCommandUnit, false);
    assertThat(ret).isEqualTo(WINDOWS_HOME_DIR);

    NgCleanupCommandUnit ngCleanupCommandUnit = NgCleanupCommandUnit.builder().build();
    ret = WinRmUtils.getWorkingDir(ngCleanupCommandUnit, true);
    assertThat(ret).isEqualTo(WINDOWS_HOME_DIR);

    ret = WinRmUtils.getWorkingDir(ngCleanupCommandUnit, false);
    assertThat(ret).isEqualTo(WINDOWS_HOME_DIR);
  }
}
