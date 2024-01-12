/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.container.ContainerResource;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("downloadAwsS3StepParameters")
@RecasterAlias("io.harness.cdng.containerStepGroup.DownloadAwsS3StepParameters")
public class DownloadAwsS3StepParameters
    extends StepGroupContainerBaseStepInfo implements SpecParameters, StepParameters {
  ParameterField<String> connectorRef;

  ParameterField<String> downloadPath;

  ParameterField<String> bucketName;

  ParameterField<String> region;

  ParameterField<List<String>> outputFilePathsContent;

  ParameterField<List<String>> paths;

  @Builder(builderMethodName = "infoBuilder")
  public DownloadAwsS3StepParameters(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      ParameterField<String> connectorRef, ContainerResource resources, ParameterField<List<String>> paths,
      ParameterField<Integer> runAsUser, ParameterField<String> downloadPath, ParameterField<String> bucketName,
      ParameterField<String> region, ParameterField<List<String>> outputFilePathsContent,
      ParameterField<Boolean> privileged) {
    super(delegateSelectors, runAsUser, resources, privileged);
    this.connectorRef = connectorRef;
    this.downloadPath = downloadPath;
    this.bucketName = bucketName;
    this.paths = paths;
    this.region = region;
    this.outputFilePathsContent = outputFilePathsContent;
  }
}
