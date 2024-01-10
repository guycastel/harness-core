/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.LicenseInfo;
import software.wings.beans.account.AccountStatus;

import java.time.Duration;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class LicenseUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  public void testIsActive(LicenseInfo licenseInfo, Duration expiredBefore, boolean isActive) {
    assertThat(LicenseUtils.isActive(licenseInfo, expiredBefore)).isEqualTo(isActive);
  }

  private Object[][] getData() {
    return new Object[][] {{LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).build(), Duration.ZERO, true},
        {LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).expiryTime(-1).build(), Duration.ZERO, true},
        {LicenseInfo.builder()
                .accountStatus(AccountStatus.ACTIVE)
                // expires 1 hour from now
                .expiryTime(System.currentTimeMillis() + Duration.ofHours(1).toMillis())
                .build(),
            Duration.ZERO, true},
        {LicenseInfo.builder()
                .accountStatus(AccountStatus.ACTIVE)
                // expired 1 hour back
                .expiryTime(System.currentTimeMillis() - Duration.ofHours(1).toMillis())
                .build(),
            Duration.ZERO, false},
        {LicenseInfo.builder()
                .accountStatus(AccountStatus.ACTIVE)
                // expired 1 hour back
                .expiryTime(System.currentTimeMillis() - Duration.ofHours(1).toMillis())
                .build(),
            Duration.ofMinutes(5), false},
        {LicenseInfo.builder()
                .accountStatus(AccountStatus.EXPIRED)
                // expired 1 hour back
                .expiryTime(System.currentTimeMillis() - Duration.ofHours(1).toMillis())
                .build(),
            Duration.ofMinutes(5), false},
        {LicenseInfo.builder()
                .accountStatus(AccountStatus.EXPIRED)
                // expired 1 hour back
                .expiryTime(System.currentTimeMillis() - Duration.ofHours(1).toMillis())
                .build(),
            Duration.ofDays(1), true}};
  }
}
