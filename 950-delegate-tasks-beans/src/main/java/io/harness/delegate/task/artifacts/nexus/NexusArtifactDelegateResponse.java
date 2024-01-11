/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.nexus;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import software.wings.utils.RepositoryFormat;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.tools.StringUtils;

@Value
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDP)
public class NexusArtifactDelegateResponse extends ArtifactDelegateResponse {
  String repositoryName;
  /** Images in repos need to be referenced via a path */
  String artifactPath;
  String repositoryFormat;
  /** Tag refers to exact tag number */
  String tag;

  Map<String, String> label;

  @Builder
  public NexusArtifactDelegateResponse(ArtifactBuildDetailsNG buildDetails, ArtifactSourceType sourceType,
      String repositoryName, String artifactPath, String repositoryFormat, String tag, Map<String, String> label) {
    super(buildDetails, sourceType);
    this.repositoryName = repositoryName;
    this.artifactPath = artifactPath;
    this.repositoryFormat = repositoryFormat;
    this.tag = tag;
    this.label = label;
  }

  @Override
  public String describe() {
    String dockerPullCommand = (RepositoryFormat.docker.name().equals(getRepositoryFormat())
                                   && getBuildDetails() != null && getBuildDetails().getMetadata() != null)
        ? "\nTo pull image use: docker pull " + getBuildDetails().getMetadata().get(ArtifactMetadataKeys.IMAGE)
        : null;
    switch (RepositoryFormat.valueOf(getRepositoryFormat())) {
      case docker:
        return "type: " + (getSourceType() != null ? getSourceType().getDisplayName() : null)
            + "\nrepository: " + getRepositoryName() + "\nimagePath: " + getArtifactPath() + "\ntag: " + getTag()
            + "\nrepository type: " + getRepositoryFormat() + StringUtils.defaultIfBlank(dockerPullCommand, "");
      case maven:
        return "type: " + (getSourceType() != null ? getSourceType().getDisplayName() : null) + "\nrepository: "
            + getRepositoryName() + "\nGroupId: " + getMetaDataValue(ArtifactMetadataKeys.groupId) + "\nArtifactPath: "
            + getArtifactPath() + "\nExtension: " + getMetaDataValue(ArtifactMetadataKeys.extension)
            + "\nClassifier: " + getMetaDataValue(ArtifactMetadataKeys.classifier) + "\ntag: " + getTag()
            + "\nrepository type: " + getRepositoryFormat();

      case npm:
      case nuget:
        return "type: " + (getSourceType() != null ? getSourceType().getDisplayName() : null) + "\nrepository: "
            + getRepositoryName() + "\npackageName: " + getMetaDataValue(ArtifactMetadataKeys.Package)
            + "\ntag: " + getTag() + "\nrepository type: " + getRepositoryFormat();
      case raw:
        return "type: " + (getSourceType() != null ? getSourceType().getDisplayName() : null) + "\nrepository: "
            + getRepositoryName() + "\ntag: " + getTag() + "\nrepository type: " + getRepositoryFormat();

      default:
        return "Unknown repository format: "
            + "\nrepository: " + null;
    }
  }

  private String getMetaDataValue(String value) {
    if (getBuildDetails() != null && getBuildDetails().getMetadata() != null) {
      return getBuildDetails().getMetadata().get(value);
    }
    return null;
  }
}
