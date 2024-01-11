/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifact;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_ARTIFACTS})
@OwnedBy(HarnessTeam.CDC)
public interface ArtifactMetadataKeys {
  String IMAGE = "image";
  String TAG = "tag";
  String REGISTRY_HOSTNAME = "registryHostname";
  String artifactPath = "artifactPath";
  String artifactName = "artifactName";
  String artifactFileSize = "artifactFileSize";
  String artifactPackage = "artifactPackage";
  String artifactProject = "artifactProject";
  String artifactRepositoryName = "artifactRepositoryName";
  String artifactRegion = "artifactRegion";
  String bucketName = "bucketName";
  String subscriptionId = "subscriptionId";
  String registry = "registry";
  String repository = "repository";
  String Package = "package";
  String packageType = "packageType";
  String feed = "feed";
  String region = "region";
  String groupId = "groupId";
  String project = "project";
  String extension = "extension";
  String classifier = "classifier";

  String FILE_NAME = "fileName";
  String IMAGE_PATH = "imagePath";
  String SHA = "SHA";
  String SHAV2 = "SHAV2";
  String url = "url";
  String bucket = "bucket";

  String artifactFileName = "artifactFileName";
  String format = "format";
  String createTime = "createTime";
  String updateTime = "updateTime";
  String id = "id";
  String planName = "planName";
}
