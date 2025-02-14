/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.exception;

import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.exception.mapper.NotFoundExceptionMapper;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.rule.Owner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NotFoundExceptionMapperTest extends CategoryTest {
  private NotFoundExceptionMapper notFoundExceptionMapper;

  @Before
  public void setup() {
    notFoundExceptionMapper = new NotFoundExceptionMapper();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToResponse() {
    NotFoundException exception = new NotFoundException();
    Response response = notFoundExceptionMapper.toResponse(exception);
    assertThat(response.getEntity()).isInstanceOf(ErrorDTO.class);
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    ErrorDTO errorDTO = (ErrorDTO) response.getEntity();
    assertThat(errorDTO.getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND_EXCEPTION);
    assertThat(errorDTO.getStatus()).isEqualTo(Status.ERROR);
  }
}
