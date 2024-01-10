/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.v1.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;

import java.util.List;
import lombok.Builder;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(PIPELINE)
public class MultiRegionArtifactTriggerConfig implements NGTriggerSpecV2, BuildAware {
  ArtifactType type;
  List<ArtifactTypeSpecWrapper> sources;

  @Builder
  public MultiRegionArtifactTriggerConfig(ArtifactType type, List<ArtifactTypeSpecWrapper> sources) {
    this.type = type;
    this.sources = sources;
  }

  @Override
  public String fetchBuildType() {
    if (isEmpty(this.sources)) {
      return null;
    }
    return this.sources.get(0).getSpec().fetchBuildType();
  }
}
