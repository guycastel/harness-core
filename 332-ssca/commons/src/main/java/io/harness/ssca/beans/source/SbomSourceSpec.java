/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.source;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = ImageSbomSource.class, name = SbomSourceConstants.IMAGE)
  , @JsonSubTypes.Type(value = RepositorySbomSource.class, name = SbomSourceConstants.REPOSITORY)
})
public interface SbomSourceSpec {}
