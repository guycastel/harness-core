/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.triggers.v1;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.Constants.ACR;
import static io.harness.ngtriggers.Constants.AMAZON_S3;
import static io.harness.ngtriggers.Constants.AMI;
import static io.harness.ngtriggers.Constants.ARTIFACTORY_REGISTRY;
import static io.harness.ngtriggers.Constants.AZURE_ARTIFACTS;
import static io.harness.ngtriggers.Constants.BAMBOO;
import static io.harness.ngtriggers.Constants.CUSTOM_ARTIFACT;
import static io.harness.ngtriggers.Constants.DOCKER_REGISTRY;
import static io.harness.ngtriggers.Constants.ECR;
import static io.harness.ngtriggers.Constants.GCR;
import static io.harness.ngtriggers.Constants.GITHUB_PACKAGES;
import static io.harness.ngtriggers.Constants.GOOGLE_ARTIFACT_REGISTRY;
import static io.harness.ngtriggers.Constants.GOOGLE_CLOUD_STORAGE;
import static io.harness.ngtriggers.Constants.JENKINS;
import static io.harness.ngtriggers.Constants.NEXUS2_REGISTRY;
import static io.harness.ngtriggers.Constants.NEXUS3_REGISTRY;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.artifact.AMIRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.AcrSpec;
import io.harness.ngtriggers.beans.source.artifact.AmazonS3RegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.ArtifactType;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTypeSpecWrapper;
import io.harness.ngtriggers.beans.source.artifact.ArtifactoryRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.AzureArtifactsRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.BambooRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.DockerRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.EcrSpec;
import io.harness.ngtriggers.beans.source.artifact.GarSpec;
import io.harness.ngtriggers.beans.source.artifact.GcrSpec;
import io.harness.ngtriggers.beans.source.artifact.GithubPackagesSpec;
import io.harness.ngtriggers.beans.source.artifact.GoolgeCloudStorageRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.JenkinsRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.MultiRegionArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.Nexus2RegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.NexusRegistrySpec;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.conditionchecker.ConditionOperator;
import io.harness.spec.server.pipeline.v1.model.AMIFilter;
import io.harness.spec.server.pipeline.v1.model.AcrArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AcrArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonMachineImageArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonMachineImageArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonS3ArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonS3ArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.ArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.ArtifactoryRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.ArtifactoryRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AzureArtifactsArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AzureArtifactsArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.BambooArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.BambooArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.CustomArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.CustomArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.DockerRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.DockerRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.EcrArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.EcrArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GcrArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GcrArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GithubPackageRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GithubPackageRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleArtifactRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleArtifactRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleCloudStorageArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleCloudStorageArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.JenkinsArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.JenkinsArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.MultiRegionArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus2RegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus2RegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus3RegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus3RegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.TriggerConditions;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableV1;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.core.variables.v1.NGVariableV1Wrapper;
import io.harness.yaml.core.variables.v1.NumberNGVariableV1;
import io.harness.yaml.core.variables.v1.SecretNGVariableV1;
import io.harness.yaml.core.variables.v1.StringNGVariableV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(HarnessTeam.PIPELINE)
public class NGArtifactTriggerApiUtils {
  ArtifactType toArtifactTriggerType(io.harness.ngtriggers.beans.source.v1.artifact.ArtifactType typeEnum) {
    switch (typeEnum) {
      case ACR:
        return ArtifactType.ACR;
      case ECR:
        return ArtifactType.ECR;
      case GCR:
        return ArtifactType.GCR;
      case BAMBOO:
        return ArtifactType.BAMBOO;
      case JENKINS:
        return ArtifactType.JENKINS;
      case AMAZON_S3:
        return ArtifactType.AMAZON_S3;
      case AZURE_ARTIFACTS:
        return ArtifactType.AZURE_ARTIFACTS;
      case CUSTOM_ARTIFACT:
        return ArtifactType.CUSTOM_ARTIFACT;
      case DOCKER_REGISTRY:
        return ArtifactType.DOCKER_REGISTRY;
      case NEXUS2_REGISTRY:
        return ArtifactType.NEXUS2_REGISTRY;
      case NEXUS3_REGISTRY:
        return ArtifactType.NEXUS3_REGISTRY;
      case AMI:
        return ArtifactType.AMI;
      case GOOGLE_CLOUD_STORAGE:
        return ArtifactType.GOOGLE_CLOUD_STORAGE;
      case ARTIFACTORY_REGISTRY:
        return ArtifactType.ARTIFACTORY_REGISTRY;
      case GITHUB_PACKAGES:
        return ArtifactType.GITHUB_PACKAGES;
      case GoogleArtifactRegistry:
        return ArtifactType.GoogleArtifactRegistry;
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + typeEnum + " is invalid");
    }
  }

  Pair<String, String> getImageAndTagFromLocation(String location) {
    String[] strings = location.split(":");
    return Pair.of(strings[0], strings[1]);
  }

  ArtifactTypeSpec toArtifactTypeSpec(io.harness.ngtriggers.beans.source.v1.artifact.ArtifactTypeSpec spec,
      io.harness.ngtriggers.beans.source.v1.artifact.ArtifactType artifactType) {
    switch (artifactType) {
      case GCR:
        io.harness.ngtriggers.beans.source.v1.artifact.GcrSpec gcrArtifactSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.GcrSpec) spec;
        return GcrSpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .imagePath(getImageAndTagFromLocation(gcrArtifactSpec.getLocation()).getLeft())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .registryHostname(gcrArtifactSpec.getHost())
            .tag(getImageAndTagFromLocation(gcrArtifactSpec.getLocation()).getRight())
            .build();
      case GoogleArtifactRegistry:
        io.harness.ngtriggers.beans.source.v1.artifact.GarSpec garArtifactSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.GarSpec) spec;
        return GarSpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .pkg(garArtifactSpec.getPkg())
            .project(garArtifactSpec.getProject())
            .region(garArtifactSpec.getRegion())
            .version(garArtifactSpec.getVersion())
            .repositoryName(garArtifactSpec.getRepo())
            .build();
      case GITHUB_PACKAGES:
        io.harness.ngtriggers.beans.source.v1.artifact.GithubPackagesSpec gprArtifactSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.GithubPackagesSpec) spec;
        return GithubPackagesSpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .org(gprArtifactSpec.getOrg())
            .packageType(gprArtifactSpec.getPkg().getType())
            .packageName(gprArtifactSpec.getPkg().getName())
            .build();
      case ARTIFACTORY_REGISTRY:
        io.harness.ngtriggers.beans.source.v1.artifact.ArtifactoryRegistrySpec artifactoryRegistryArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.ArtifactoryRegistrySpec) spec;
        return ArtifactoryRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .artifactDirectory(artifactoryRegistryArtifactTriggerSpec.getDir())
            .artifactFilter(artifactoryRegistryArtifactTriggerSpec.getFilter())
            .artifactPath(artifactoryRegistryArtifactTriggerSpec.getPath())
            .repository(artifactoryRegistryArtifactTriggerSpec.getRepo().getName())
            .repositoryFormat(artifactoryRegistryArtifactTriggerSpec.getRepo().getFormat())
            .repositoryUrl(artifactoryRegistryArtifactTriggerSpec.getRepo().getUrl())
            .build();
      case GOOGLE_CLOUD_STORAGE:
        io.harness.ngtriggers.beans.source.v1.artifact.GoolgeCloudStorageRegistrySpec gcsArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.GoolgeCloudStorageRegistrySpec) spec;
        return GoolgeCloudStorageRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .artifactPath(gcsArtifactTriggerSpec.getPath())
            .bucket(gcsArtifactTriggerSpec.getBucket())
            .project(gcsArtifactTriggerSpec.getProject())
            .build();
      case AMI:
        io.harness.ngtriggers.beans.source.v1.artifact.AMIRegistrySpec amiArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.AMIRegistrySpec) spec;
        return AMIRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .filters(amiArtifactTriggerSpec.getFilters())
            .region(amiArtifactTriggerSpec.getRegion())
            .tags(amiArtifactTriggerSpec.getTags())
            .version(amiArtifactTriggerSpec.getVersion())
            .versionRegex(amiArtifactTriggerSpec.getVersion_regex())
            .build();
      case NEXUS3_REGISTRY:
        io.harness.ngtriggers.beans.source.v1.artifact.NexusRegistrySpec nexus3ArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.NexusRegistrySpec) spec;
        return NexusRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .artifactId(nexus3ArtifactTriggerSpec.getArtifact())
            .classifier(nexus3ArtifactTriggerSpec.getClassifier())
            .extension(nexus3ArtifactTriggerSpec.getExtension())
            .group(nexus3ArtifactTriggerSpec.getGroup())
            .groupId(nexus3ArtifactTriggerSpec.getGroup_id())
            .imagePath(getImageAndTagFromLocation(nexus3ArtifactTriggerSpec.getLocation()).getLeft())
            .packageName(nexus3ArtifactTriggerSpec.getPkg())
            .repository(nexus3ArtifactTriggerSpec.getRepo().getName())
            .repositoryFormat(nexus3ArtifactTriggerSpec.getRepo().getFormat())
            .repositoryUrl(nexus3ArtifactTriggerSpec.getRepo().getUrl())
            .tag(getImageAndTagFromLocation(nexus3ArtifactTriggerSpec.getLocation()).getRight())
            .build();
      case DOCKER_REGISTRY:
        io.harness.ngtriggers.beans.source.v1.artifact.DockerRegistrySpec dockerRegistryArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.DockerRegistrySpec) spec;
        return DockerRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .imagePath(getImageAndTagFromLocation(dockerRegistryArtifactTriggerSpec.getLocation()).getLeft())
            .tag(getImageAndTagFromLocation(dockerRegistryArtifactTriggerSpec.getLocation()).getRight())
            .build();
      case CUSTOM_ARTIFACT:
        io.harness.ngtriggers.beans.source.v1.artifact.CustomArtifactSpec customArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.CustomArtifactSpec) spec;
        return io.harness.ngtriggers.beans.source.artifact.CustomArtifactSpec.builder()
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .artifactsArrayPath(customArtifactTriggerSpec.getPath())
            .versionPath(customArtifactTriggerSpec.getVersion_path())
            .inputs(toNGVariableList(customArtifactTriggerSpec.getInputs()))
            .version(customArtifactTriggerSpec.getVersion())
            .metadata(customArtifactTriggerSpec.getMetadata())
            .script(customArtifactTriggerSpec.getScript())
            .build();
      case NEXUS2_REGISTRY:
        io.harness.ngtriggers.beans.source.v1.artifact.Nexus2RegistrySpec nexus2ArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.Nexus2RegistrySpec) spec;
        return Nexus2RegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .artifactId(nexus2ArtifactTriggerSpec.getArtifact())
            .classifier(nexus2ArtifactTriggerSpec.getClassifier())
            .extension(nexus2ArtifactTriggerSpec.getExtension())
            .groupId(nexus2ArtifactTriggerSpec.getGroup_id())
            .packageName(nexus2ArtifactTriggerSpec.getPkg())
            .repositoryFormat(nexus2ArtifactTriggerSpec.getRepo().getFormat())
            .repositoryUrl(nexus2ArtifactTriggerSpec.getRepo().getUrl())
            .tag(nexus2ArtifactTriggerSpec.getTag())
            .repositoryName(nexus2ArtifactTriggerSpec.getRepo().getName())
            .build();
      case AZURE_ARTIFACTS:
        io.harness.ngtriggers.beans.source.v1.artifact.AzureArtifactsRegistrySpec azureArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.AzureArtifactsRegistrySpec) spec;
        return AzureArtifactsRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .packageName(azureArtifactTriggerSpec.getPkg().getName())
            .feed(azureArtifactTriggerSpec.getFeed())
            .packageType(azureArtifactTriggerSpec.getPkg().getType())
            .project(azureArtifactTriggerSpec.getProject())
            .version(azureArtifactTriggerSpec.getVersion())
            .versionRegex(azureArtifactTriggerSpec.getVersion_regex())
            .build();
      case AMAZON_S3:
        io.harness.ngtriggers.beans.source.v1.artifact.AmazonS3RegistrySpec amazonS3ArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.AmazonS3RegistrySpec) spec;
        return AmazonS3RegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .bucketName(amazonS3ArtifactTriggerSpec.getBucket())
            .filePathRegex(amazonS3ArtifactTriggerSpec.getPath_regex())
            .region(amazonS3ArtifactTriggerSpec.getRegion())
            .build();
      case JENKINS:
        io.harness.ngtriggers.beans.source.v1.artifact.JenkinsRegistrySpec jenkinsArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.JenkinsRegistrySpec) spec;
        return JenkinsRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .artifactPath(jenkinsArtifactTriggerSpec.getPath())
            .jobName(jenkinsArtifactTriggerSpec.getJob())
            .build(jenkinsArtifactTriggerSpec.getBuild())
            .build();
      case BAMBOO:
        io.harness.ngtriggers.beans.source.v1.artifact.BambooRegistrySpec bambooArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.BambooRegistrySpec) spec;
        return BambooRegistrySpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .planKey(bambooArtifactTriggerSpec.getPlan_key())
            .artifactPaths(bambooArtifactTriggerSpec.getPaths())
            .build(bambooArtifactTriggerSpec.getBuild())
            .build();
      case ECR:
        io.harness.ngtriggers.beans.source.v1.artifact.EcrSpec ecrArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.EcrSpec) spec;
        return EcrSpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .imagePath(getImageAndTagFromLocation(ecrArtifactTriggerSpec.getLocation()).getLeft())
            .region(ecrArtifactTriggerSpec.getRegion())
            .tag(getImageAndTagFromLocation(ecrArtifactTriggerSpec.getLocation()).getRight())
            .registryId(ecrArtifactTriggerSpec.getRegistry())
            .build();
      case ACR:
        io.harness.ngtriggers.beans.source.v1.artifact.AcrSpec acrArtifactTriggerSpec =
            (io.harness.ngtriggers.beans.source.v1.artifact.AcrSpec) spec;
        return AcrSpec.builder()
            .connectorRef(spec.fetchConnectorRef())
            .eventConditions(spec.fetchEventDataConditions())
            .jexlCondition(spec.fetchJexlArtifactConditions())
            .metaDataConditions(spec.fetchMetaDataConditions())
            .tag(acrArtifactTriggerSpec.getTag())
            .registry(acrArtifactTriggerSpec.getRegistry())
            .repository(acrArtifactTriggerSpec.getRepo())
            .subscriptionId(acrArtifactTriggerSpec.getSubscription())
            .build();
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + artifactType + " is invalid");
    }
  }

  List<NGVariable> toNGVariableList(NGVariableV1Wrapper ngVariableV1Wrapper) {
    List<NGVariable> variablesList = new ArrayList<>();
    if (ngVariableV1Wrapper == null || isEmpty(ngVariableV1Wrapper.getMap())) {
      return variablesList;
    }
    Map<String, NGVariableV1> variables = ngVariableV1Wrapper.getMap();
    for (Map.Entry<String, NGVariableV1> entry : variables.entrySet()) {
      switch (entry.getValue().getType()) {
        case STRING:
          StringNGVariableV1 stringNGVariableV1 = (StringNGVariableV1) entry.getValue();
          variablesList.add(StringNGVariable.builder()
                                .defaultValue(stringNGVariableV1.getDefaultValue())
                                .value(stringNGVariableV1.getValue())
                                .name(entry.getKey())
                                .required(stringNGVariableV1.isRequired())
                                .description(stringNGVariableV1.getDesc())
                                .build());
          break;
        case NUMBER:
          NumberNGVariableV1 numberNGVariableV1 = (NumberNGVariableV1) entry.getValue();
          variablesList.add(NumberNGVariable.builder()
                                .defaultValue(numberNGVariableV1.getDefaultValue())
                                .value(numberNGVariableV1.getValue())
                                .name(entry.getKey())
                                .required(numberNGVariableV1.isRequired())
                                .description(numberNGVariableV1.getDesc())
                                .build());
          break;
        case SECRET:
          SecretNGVariableV1 secretNGVariableV1 = (SecretNGVariableV1) entry.getValue();
          variablesList.add(SecretNGVariable.builder()
                                .defaultValue(secretNGVariableV1.getDefaultValue())
                                .value(secretNGVariableV1.getValue())
                                .name(entry.getKey())
                                .required(secretNGVariableV1.isRequired())
                                .description(secretNGVariableV1.getDesc())
                                .build());
          break;
        default:
          throw new InvalidRequestException("Variable type " + entry.getValue().getType() + "is invalid");
      }
    }
    return variablesList;
  }

  MultiRegionArtifactTriggerSpec toMultiRegionArtifactTriggerSpec(NGTriggerSpecV2 spec) {
    MultiRegionArtifactTriggerConfig multiRegionArtifactTriggerConfig = (MultiRegionArtifactTriggerConfig) spec;
    MultiRegionArtifactTriggerSpec multiRegionArtifactTriggerSpec = new MultiRegionArtifactTriggerSpec();
    multiRegionArtifactTriggerSpec.setEventConditions(multiRegionArtifactTriggerConfig.getEventConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
    multiRegionArtifactTriggerSpec.setJexlCondition(multiRegionArtifactTriggerConfig.getJexlCondition());
    multiRegionArtifactTriggerSpec.setSources(multiRegionArtifactTriggerConfig.getSources()
                                                  .stream()
                                                  .map(this::toArtifactTypeSpecWrapper)
                                                  .collect(Collectors.toList()));
    multiRegionArtifactTriggerSpec.setType(toArtifactType(multiRegionArtifactTriggerConfig.getType()));
    multiRegionArtifactTriggerSpec.setMetaDataConditions(multiRegionArtifactTriggerConfig.getMetaDataConditions()
                                                             .stream()
                                                             .map(this::toTriggerCondition)
                                                             .collect(Collectors.toList()));
    return multiRegionArtifactTriggerSpec;
  }

  io.harness.spec.server.pipeline.v1.model.ArtifactTypeSpecWrapper toArtifactTypeSpecWrapper(
      ArtifactTypeSpecWrapper artifactTypeSpecWrapper) {
    io.harness.spec.server.pipeline.v1.model.ArtifactTypeSpecWrapper artifactTypeSpecWrapper1 =
        new io.harness.spec.server.pipeline.v1.model.ArtifactTypeSpecWrapper();
    artifactTypeSpecWrapper1.setSpec(toArtifactTriggerSpec(artifactTypeSpecWrapper.getSpec()));
    return artifactTypeSpecWrapper1;
  }

  io.harness.spec.server.pipeline.v1.model.ArtifactType toArtifactType(ArtifactType type) {
    switch (type) {
      case GOOGLE_CLOUD_STORAGE:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GOOGLECLOUDSTORAGE;
      case ARTIFACTORY_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.ARTIFACTORYREGISTRY;
      case NEXUS3_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS3REGISTRY;
      case NEXUS2_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS2REGISTRY;
      case GITHUB_PACKAGES:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY;
      case DOCKER_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.DOCKERREGISTRY;
      case CUSTOM_ARTIFACT:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.CUSTOMARTIFACT;
      case AZURE_ARTIFACTS:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.AZUREARTIFACTS;
      case AMAZON_S3:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONS3;
      case AMI:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONMACHINEIMAGE;
      case GCR:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GCR;
      case JENKINS:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.JENKINS;
      case BAMBOO:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.BAMBOO;
      case ECR:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.ECR;
      case ACR:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.ACR;
      case GoogleArtifactRegistry:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GOOGLEARTIFACTREGISTRY;
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + type + " is invalid");
    }
  }

  GoogleArtifactRegistryArtifactTriggerSpec toGoogleArtifactRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GarSpec garSpec = (GarSpec) spec;
    GoogleArtifactRegistryArtifactTriggerSpec googleArtifactRegistryArtifactTriggerSpec =
        new GoogleArtifactRegistryArtifactTriggerSpec();
    googleArtifactRegistryArtifactTriggerSpec.setConnectorRef(garSpec.getConnectorRef());
    googleArtifactRegistryArtifactTriggerSpec.setEventConditions(
        garSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    googleArtifactRegistryArtifactTriggerSpec.setJexlCondition(garSpec.getJexlCondition());
    googleArtifactRegistryArtifactTriggerSpec.setMetaDataConditions(
        garSpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    googleArtifactRegistryArtifactTriggerSpec.setPkg(garSpec.getPkg());
    googleArtifactRegistryArtifactTriggerSpec.setProject(garSpec.getProject());
    googleArtifactRegistryArtifactTriggerSpec.setVersion(garSpec.getVersion());
    googleArtifactRegistryArtifactTriggerSpec.setRepositoryName(garSpec.getRepositoryName());
    googleArtifactRegistryArtifactTriggerSpec.setRegion(garSpec.getRegion());
    return googleArtifactRegistryArtifactTriggerSpec;
  }

  AcrArtifactTriggerSpec toAcrArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AcrSpec acrSpec = (AcrSpec) spec;
    AcrArtifactTriggerSpec acrArtifactTriggerSpec = new AcrArtifactTriggerSpec();
    acrArtifactTriggerSpec.setConnectorRef(acrSpec.getConnectorRef());
    acrArtifactTriggerSpec.setEventConditions(
        acrSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    acrArtifactTriggerSpec.setJexlCondition(acrSpec.getJexlCondition());
    acrArtifactTriggerSpec.setMetaDataConditions(
        acrSpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    acrArtifactTriggerSpec.setRegistry(acrSpec.getRegistry());
    acrArtifactTriggerSpec.setTag(acrSpec.getTag());
    acrArtifactTriggerSpec.setSubscriptionId(acrSpec.getSubscriptionId());
    acrArtifactTriggerSpec.setRepository(acrSpec.getRepository());
    return acrArtifactTriggerSpec;
  }

  EcrArtifactTriggerSpec toEcrArtifactTriggerSpec(ArtifactTypeSpec spec) {
    EcrSpec ecrSpec = (EcrSpec) spec;
    EcrArtifactTriggerSpec ecrArtifactTriggerSpec = new EcrArtifactTriggerSpec();
    ecrArtifactTriggerSpec.setConnectorRef(ecrSpec.getConnectorRef());
    ecrArtifactTriggerSpec.setEventConditions(
        ecrSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    ecrArtifactTriggerSpec.setJexlCondition(ecrSpec.getJexlCondition());
    ecrArtifactTriggerSpec.setMetaDataConditions(
        ecrSpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    ecrArtifactTriggerSpec.setTag(ecrSpec.getTag());
    ecrArtifactTriggerSpec.setRegion(ecrSpec.getRegion());
    ecrArtifactTriggerSpec.setRegistryId(ecrSpec.getRegistryId());
    ecrArtifactTriggerSpec.imagePath(ecrSpec.getImagePath());
    return ecrArtifactTriggerSpec;
  }

  BambooArtifactTriggerSpec toBambooArtifactTriggerSpec(ArtifactTypeSpec spec) {
    BambooRegistrySpec bambooArtifactSpec = (BambooRegistrySpec) spec;
    BambooArtifactTriggerSpec bambooArtifactTriggerSpec = new BambooArtifactTriggerSpec();
    bambooArtifactTriggerSpec.setArtifactPaths(bambooArtifactSpec.getArtifactPaths());
    bambooArtifactTriggerSpec.setConnectorRef(bambooArtifactSpec.getConnectorRef());
    bambooArtifactTriggerSpec.setEventConditions(
        bambooArtifactSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    bambooArtifactTriggerSpec.setJexlCondition(bambooArtifactSpec.getJexlCondition());
    bambooArtifactTriggerSpec.setMetaDataConditions(
        bambooArtifactSpec.getMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    bambooArtifactTriggerSpec.setPlanKey(bambooArtifactSpec.getPlanKey());
    bambooArtifactTriggerSpec.setBuild(bambooArtifactSpec.getBuild());
    return bambooArtifactTriggerSpec;
  }

  JenkinsArtifactTriggerSpec toJenkinsArtifactTriggerSpec(ArtifactTypeSpec spec) {
    JenkinsRegistrySpec jenkinsRegistrySpec = (JenkinsRegistrySpec) spec;
    JenkinsArtifactTriggerSpec jenkinsArtifactTriggerSpec = new JenkinsArtifactTriggerSpec();
    jenkinsArtifactTriggerSpec.setArtifactPath(jenkinsRegistrySpec.getArtifactPath());
    jenkinsArtifactTriggerSpec.setBuild(jenkinsRegistrySpec.getBuild());
    jenkinsArtifactTriggerSpec.setConnectorRef(jenkinsRegistrySpec.getConnectorRef());
    jenkinsArtifactTriggerSpec.setEventConditions(
        jenkinsRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    jenkinsArtifactTriggerSpec.setMetaDataConditions(jenkinsRegistrySpec.fetchMetaDataConditions()
                                                         .stream()
                                                         .map(this::toTriggerCondition)
                                                         .collect(Collectors.toList()));
    jenkinsArtifactTriggerSpec.setJobName(jenkinsRegistrySpec.getJobName());
    jenkinsArtifactTriggerSpec.setJexlCondition(jenkinsRegistrySpec.getJexlCondition());
    return jenkinsArtifactTriggerSpec;
  }

  GcrArtifactTriggerSpec toGcrArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GcrSpec gcrSpec = (GcrSpec) spec;
    GcrArtifactTriggerSpec gcrArtifactTriggerSpec = new GcrArtifactTriggerSpec();
    gcrArtifactTriggerSpec.setConnectorRef(gcrSpec.getConnectorRef());
    gcrArtifactTriggerSpec.setEventConditions(
        gcrSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    gcrArtifactTriggerSpec.setJexlCondition(gcrSpec.getJexlCondition());
    gcrArtifactTriggerSpec.setRegistryHostname(gcrSpec.getRegistryHostname());
    gcrArtifactTriggerSpec.setImagePath(gcrSpec.getImagePath());
    gcrArtifactTriggerSpec.setTag(gcrSpec.getTag());
    gcrArtifactTriggerSpec.setRegistryHostname(gcrSpec.getRegistryHostname());
    gcrArtifactTriggerSpec.setMetaDataConditions(
        gcrSpec.getMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    return gcrArtifactTriggerSpec;
  }

  AmazonMachineImageArtifactTriggerSpec toAmazonMachineImageArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AMIRegistrySpec amiRegistrySpec = (AMIRegistrySpec) spec;
    AmazonMachineImageArtifactTriggerSpec amazonMachineImageArtifactTriggerSpec =
        new AmazonMachineImageArtifactTriggerSpec();
    amazonMachineImageArtifactTriggerSpec.setConnectorRef(amiRegistrySpec.getConnectorRef());
    amazonMachineImageArtifactTriggerSpec.setEventConditions(
        amiRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setJexlCondition(amiRegistrySpec.getJexlCondition());
    amazonMachineImageArtifactTriggerSpec.setRegion(amiRegistrySpec.getRegion());
    amazonMachineImageArtifactTriggerSpec.setMetaDataConditions(
        amiRegistrySpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setFilters(
        amiRegistrySpec.getFilters().stream().map(this::toApiAMIFilter).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setTags(
        amiRegistrySpec.getTags().stream().map(this::toApiAMIFilter).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setVersion(amiRegistrySpec.getVersion());
    amazonMachineImageArtifactTriggerSpec.setVersionRegex(amiRegistrySpec.getVersionRegex());
    return amazonMachineImageArtifactTriggerSpec;
  }

  AMIFilter toApiAMIFilter(io.harness.delegate.task.artifacts.ami.AMIFilter amiFilter) {
    AMIFilter amiFilter1 = new AMIFilter();
    amiFilter1.setName(amiFilter.getName());
    amiFilter1.setValue(amiFilter.getValue());
    return amiFilter1;
  }

  AMIFilter toApiAMIFilter(io.harness.delegate.task.artifacts.ami.AMITag amiTag) {
    AMIFilter amiFilter1 = new AMIFilter();
    amiFilter1.setName(amiTag.getName());
    amiFilter1.setValue(amiTag.getValue());
    return amiFilter1;
  }

  AmazonS3ArtifactTriggerSpec toAmazonS3ArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AmazonS3RegistrySpec amazonS3RegistrySpec = (AmazonS3RegistrySpec) spec;
    AmazonS3ArtifactTriggerSpec amazonS3ArtifactTriggerSpec = new AmazonS3ArtifactTriggerSpec();
    amazonS3ArtifactTriggerSpec.setConnectorRef(amazonS3RegistrySpec.getConnectorRef());
    amazonS3ArtifactTriggerSpec.setEventConditions(
        amazonS3RegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    amazonS3ArtifactTriggerSpec.setJexlCondition(amazonS3RegistrySpec.getJexlCondition());
    amazonS3ArtifactTriggerSpec.setRegion(amazonS3RegistrySpec.getRegion());
    amazonS3ArtifactTriggerSpec.setBucketName(amazonS3RegistrySpec.getBucketName());
    amazonS3ArtifactTriggerSpec.setFilePathRegex(amazonS3RegistrySpec.getFilePathRegex());
    amazonS3ArtifactTriggerSpec.setMetaDataConditions(amazonS3RegistrySpec.fetchMetaDataConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
    return amazonS3ArtifactTriggerSpec;
  }

  AzureArtifactsArtifactTriggerSpec toAzureArtifactsArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AzureArtifactsRegistrySpec azureArtifactsRegistrySpec = (AzureArtifactsRegistrySpec) spec;
    AzureArtifactsArtifactTriggerSpec azureArtifactsArtifactTriggerSpec = new AzureArtifactsArtifactTriggerSpec();
    azureArtifactsArtifactTriggerSpec.setConnectorRef(azureArtifactsRegistrySpec.getConnectorRef());
    azureArtifactsArtifactTriggerSpec.setEventConditions(azureArtifactsRegistrySpec.getEventConditions()
                                                             .stream()
                                                             .map(this::toTriggerCondition)
                                                             .collect(Collectors.toList()));
    azureArtifactsArtifactTriggerSpec.setJexlCondition(azureArtifactsRegistrySpec.getJexlCondition());
    azureArtifactsArtifactTriggerSpec.setMetaDataConditions(azureArtifactsRegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    azureArtifactsArtifactTriggerSpec.setVersion(azureArtifactsRegistrySpec.getVersion());
    azureArtifactsArtifactTriggerSpec.setFeed(azureArtifactsRegistrySpec.getFeed());
    azureArtifactsArtifactTriggerSpec.setPackageName(azureArtifactsRegistrySpec.getPackageName());
    azureArtifactsArtifactTriggerSpec.setPackageType(azureArtifactsRegistrySpec.getPackageType());
    azureArtifactsArtifactTriggerSpec.setProject(azureArtifactsRegistrySpec.getProject());
    azureArtifactsArtifactTriggerSpec.setVersionRegex(azureArtifactsRegistrySpec.getVersionRegex());
    return azureArtifactsArtifactTriggerSpec;
  }

  CustomArtifactTriggerSpec toCustomArtifactTriggerSpec(ArtifactTypeSpec spec) {
    io.harness.ngtriggers.beans.source.artifact.CustomArtifactSpec customArtifactSpec =
        (io.harness.ngtriggers.beans.source.artifact.CustomArtifactSpec) spec;
    CustomArtifactTriggerSpec customArtifactTriggerSpec = new CustomArtifactTriggerSpec();
    customArtifactTriggerSpec.setArtifactsArrayPath(customArtifactSpec.getArtifactsArrayPath());
    customArtifactTriggerSpec.setEventConditions(
        customArtifactSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    customArtifactTriggerSpec.setJexlCondition(customArtifactSpec.getJexlCondition());
    customArtifactTriggerSpec.setMetaDataConditions(customArtifactSpec.fetchMetaDataConditions()
                                                        .stream()
                                                        .map(this::toTriggerCondition)
                                                        .collect(Collectors.toList()));
    customArtifactTriggerSpec.setVersion(customArtifactSpec.getVersion());
    customArtifactTriggerSpec.setVersionPath(customArtifactSpec.getVersionPath());
    customArtifactTriggerSpec.setScript(customArtifactSpec.getScript());
    customArtifactTriggerSpec.setInputs(
        customArtifactSpec.getInputs().stream().map(this::toApiNGVariable).collect(Collectors.toList()));
    customArtifactTriggerSpec.setMetadata(customArtifactSpec.getMetadata());
    return customArtifactTriggerSpec;
  }

  io.harness.spec.server.pipeline.v1.model.NGVariable toApiNGVariable(NGVariable variable) {
    switch (variable.getType()) {
      case NUMBER:
        NumberNGVariable numberNGVariable1 = (NumberNGVariable) variable;
        io.harness.spec.server.pipeline.v1.model.NumberNGVariable numberNGVariable =
            new io.harness.spec.server.pipeline.v1.model.NumberNGVariable();
        numberNGVariable.setType(io.harness.spec.server.pipeline.v1.model.NGVariable.TypeEnum.NUMBER);
        numberNGVariable.setName(numberNGVariable1.getName());
        numberNGVariable.setMetadata(numberNGVariable1.getMetadata());
        numberNGVariable.setValue(numberNGVariable1.getDefaultValue());
        numberNGVariable.setRequired(numberNGVariable1.isRequired());
        numberNGVariable.setDescription(numberNGVariable1.getDescription());
        numberNGVariable.setDefaultValue(numberNGVariable1.getDefaultValue());
        return numberNGVariable;
      case STRING:
        StringNGVariable stringNGVariable1 = (StringNGVariable) variable;
        io.harness.spec.server.pipeline.v1.model.StringNGVariable stringNGVariable =
            new io.harness.spec.server.pipeline.v1.model.StringNGVariable();
        stringNGVariable.setType(io.harness.spec.server.pipeline.v1.model.NGVariable.TypeEnum.STRING);
        stringNGVariable.setName(stringNGVariable1.getName());
        stringNGVariable.setMetadata(stringNGVariable1.getMetadata());
        stringNGVariable.setValue(stringNGVariable1.getDefaultValue());
        stringNGVariable.setRequired(stringNGVariable1.isRequired());
        stringNGVariable.setDescription(stringNGVariable1.getDescription());
        stringNGVariable.setDefaultValue(stringNGVariable1.getDefaultValue());
        return stringNGVariable;
      default:
        throw new InvalidRequestException("Variable Type " + variable.getType() + " is invalid");
    }
  }

  DockerRegistryArtifactTriggerSpec toDockerRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    DockerRegistrySpec dockerRegistrySpec = (DockerRegistrySpec) spec;
    DockerRegistryArtifactTriggerSpec dockerRegistryArtifactTriggerSpec = new DockerRegistryArtifactTriggerSpec();
    dockerRegistryArtifactTriggerSpec.setConnectorRef(dockerRegistrySpec.getConnectorRef());
    dockerRegistryArtifactTriggerSpec.setEventConditions(
        dockerRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    dockerRegistryArtifactTriggerSpec.setJexlCondition(dockerRegistrySpec.getJexlCondition());
    dockerRegistryArtifactTriggerSpec.setTag(dockerRegistrySpec.getTag());
    dockerRegistryArtifactTriggerSpec.setImagePath(dockerRegistrySpec.getImagePath());
    dockerRegistryArtifactTriggerSpec.setMetaDataConditions(dockerRegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    return dockerRegistryArtifactTriggerSpec;
  }

  GithubPackageRegistryArtifactTriggerSpec toGithubPackageRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GithubPackagesSpec githubPackagesSpec = (GithubPackagesSpec) spec;
    GithubPackageRegistryArtifactTriggerSpec githubPackageRegistryArtifactTriggerSpec =
        new GithubPackageRegistryArtifactTriggerSpec();
    githubPackageRegistryArtifactTriggerSpec.setConnectorRef(githubPackagesSpec.getConnectorRef());
    githubPackageRegistryArtifactTriggerSpec.setPackageName(githubPackagesSpec.getPackageName());
    githubPackageRegistryArtifactTriggerSpec.setPackageType(githubPackagesSpec.getPackageType());
    githubPackageRegistryArtifactTriggerSpec.setEventConditions(
        githubPackagesSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    githubPackageRegistryArtifactTriggerSpec.setOrg(githubPackagesSpec.getOrg());
    githubPackageRegistryArtifactTriggerSpec.setMetaDataConditions(githubPackagesSpec.fetchMetaDataConditions()
                                                                       .stream()
                                                                       .map(this::toTriggerCondition)
                                                                       .collect(Collectors.toList()));
    githubPackageRegistryArtifactTriggerSpec.setJexlCondition(githubPackagesSpec.getJexlCondition());
    return githubPackageRegistryArtifactTriggerSpec;
  }

  Nexus2RegistryArtifactTriggerSpec toNexus2RegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    Nexus2RegistrySpec nexus2RegistrySpec = (Nexus2RegistrySpec) spec;
    Nexus2RegistryArtifactTriggerSpec nexus2RegistryArtifactTriggerSpec = new Nexus2RegistryArtifactTriggerSpec();
    nexus2RegistryArtifactTriggerSpec.setArtifactId(nexus2RegistrySpec.getArtifactId());
    nexus2RegistryArtifactTriggerSpec.setConnectorRef(nexus2RegistrySpec.getConnectorRef());
    nexus2RegistryArtifactTriggerSpec.setEventConditions(
        nexus2RegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    nexus2RegistryArtifactTriggerSpec.setJexlCondition(nexus2RegistrySpec.getJexlCondition());
    nexus2RegistryArtifactTriggerSpec.setMetaDataConditions(nexus2RegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    nexus2RegistryArtifactTriggerSpec.setClassifier(nexus2RegistrySpec.getClassifier());
    nexus2RegistryArtifactTriggerSpec.setExtension(nexus2RegistrySpec.getExtension());
    nexus2RegistryArtifactTriggerSpec.setGroupId(nexus2RegistrySpec.getGroupId());
    nexus2RegistryArtifactTriggerSpec.setPackageName(nexus2RegistrySpec.getPackageName());
    nexus2RegistryArtifactTriggerSpec.setRepositoryFormat(nexus2RegistrySpec.getRepositoryFormat());
    nexus2RegistryArtifactTriggerSpec.setRepositoryName(nexus2RegistrySpec.getRepositoryName());
    nexus2RegistryArtifactTriggerSpec.setRepositoryUrl(nexus2RegistrySpec.getRepositoryUrl());
    return nexus2RegistryArtifactTriggerSpec;
  }

  Nexus3RegistryArtifactTriggerSpec toNexus3RegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    NexusRegistrySpec nexusRegistrySpec = (NexusRegistrySpec) spec;
    Nexus3RegistryArtifactTriggerSpec nexus3RegistryArtifactTriggerSpec = new Nexus3RegistryArtifactTriggerSpec();
    nexus3RegistryArtifactTriggerSpec.setArtifactId(nexusRegistrySpec.getArtifactId());
    nexus3RegistryArtifactTriggerSpec.setExtension(nexusRegistrySpec.getExtension());
    nexus3RegistryArtifactTriggerSpec.setConnectorRef(nexusRegistrySpec.getConnectorRef());
    nexus3RegistryArtifactTriggerSpec.setPackageName(nexusRegistrySpec.getPackageName());
    nexus3RegistryArtifactTriggerSpec.setEventConditions(
        nexusRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    nexus3RegistryArtifactTriggerSpec.setClassifier(nexusRegistrySpec.getClassifier());
    nexus3RegistryArtifactTriggerSpec.setGroup(nexusRegistrySpec.getGroup());
    nexus3RegistryArtifactTriggerSpec.setGroupId(nexusRegistrySpec.getGroupId());
    nexus3RegistryArtifactTriggerSpec.setImagePath(nexusRegistrySpec.getImagePath());
    nexus3RegistryArtifactTriggerSpec.setJexlCondition(nexusRegistrySpec.getJexlCondition());
    nexus3RegistryArtifactTriggerSpec.setMetaDataConditions(nexusRegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    nexus3RegistryArtifactTriggerSpec.setTag(nexusRegistrySpec.getTag());
    nexus3RegistryArtifactTriggerSpec.setRepositoryUrl(nexusRegistrySpec.getRepositoryUrl());
    nexus3RegistryArtifactTriggerSpec.setRepositoryFormat(nexusRegistrySpec.getRepositoryFormat());
    nexus3RegistryArtifactTriggerSpec.setRepository(nexusRegistrySpec.getRepository());
    return nexus3RegistryArtifactTriggerSpec;
  }

  ArtifactoryRegistryArtifactTriggerSpec toArtifactoryRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    ArtifactoryRegistrySpec artifactoryRegistrySpec = (ArtifactoryRegistrySpec) spec;
    ArtifactoryRegistryArtifactTriggerSpec artifactTriggerSpec = new ArtifactoryRegistryArtifactTriggerSpec();
    artifactTriggerSpec.setArtifactDirectory(artifactoryRegistrySpec.getArtifactDirectory());
    artifactTriggerSpec.setArtifactPath(artifactoryRegistrySpec.getArtifactPath());
    artifactTriggerSpec.setArtifactFilter(artifactoryRegistrySpec.getArtifactFilter());
    artifactTriggerSpec.setRepositoryUrl(artifactoryRegistrySpec.getRepositoryUrl());
    artifactTriggerSpec.setConnectorRef(artifactoryRegistrySpec.getConnectorRef());
    artifactTriggerSpec.setRepositoryFormat(artifactoryRegistrySpec.getRepositoryFormat());
    artifactTriggerSpec.setMetaDataConditions(artifactoryRegistrySpec.fetchMetaDataConditions()
                                                  .stream()
                                                  .map(this::toTriggerCondition)
                                                  .collect(Collectors.toList()));
    artifactTriggerSpec.setJexlCondition(artifactoryRegistrySpec.getJexlCondition());
    artifactTriggerSpec.setEventConditions(artifactoryRegistrySpec.getEventConditions()
                                               .stream()
                                               .map(this::toTriggerCondition)
                                               .collect(Collectors.toList()));
    artifactTriggerSpec.setRepository(artifactoryRegistrySpec.getRepository());
    return artifactTriggerSpec;
  }

  GoogleCloudStorageArtifactTriggerSpec toGoogleCloudStorageArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GoolgeCloudStorageRegistrySpec goolgeCloudStorageRegistrySpec = (GoolgeCloudStorageRegistrySpec) spec;
    GoogleCloudStorageArtifactTriggerSpec googleCloudStorageArtifactTriggerSpec =
        new GoogleCloudStorageArtifactTriggerSpec();
    googleCloudStorageArtifactTriggerSpec.setArtifactPath(goolgeCloudStorageRegistrySpec.getArtifactPath());
    googleCloudStorageArtifactTriggerSpec.setConnectorRef(goolgeCloudStorageRegistrySpec.getConnectorRef());
    googleCloudStorageArtifactTriggerSpec.setEventConditions(goolgeCloudStorageRegistrySpec.getEventConditions()
                                                                 .stream()
                                                                 .map(this::toTriggerCondition)
                                                                 .collect(Collectors.toList()));
    googleCloudStorageArtifactTriggerSpec.setMetaDataConditions(goolgeCloudStorageRegistrySpec.fetchMetaDataConditions()
                                                                    .stream()
                                                                    .map(this::toTriggerCondition)
                                                                    .collect(Collectors.toList()));
    googleCloudStorageArtifactTriggerSpec.setProject(goolgeCloudStorageRegistrySpec.getProject());
    googleCloudStorageArtifactTriggerSpec.setBucket(goolgeCloudStorageRegistrySpec.getBucket());
    googleCloudStorageArtifactTriggerSpec.setJexlCondition(goolgeCloudStorageRegistrySpec.getJexlCondition());
    return googleCloudStorageArtifactTriggerSpec;
  }

  ArtifactTriggerSpec toArtifactTriggerSpec(ArtifactTypeSpec spec) {
    switch (spec.fetchBuildType()) {
      case GOOGLE_ARTIFACT_REGISTRY:
        GoogleArtifactRegistryArtifactSpec googleArtifactRegistryArtifactSpec =
            new GoogleArtifactRegistryArtifactSpec();
        googleArtifactRegistryArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GOOGLEARTIFACTREGISTRY);
        googleArtifactRegistryArtifactSpec.setSpec(toGoogleArtifactRegistryArtifactTriggerSpec(spec));
        return googleArtifactRegistryArtifactSpec;
      case ACR:
        AcrArtifactSpec acrArtifactSpec = new AcrArtifactSpec();
        acrArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.ACR);
        acrArtifactSpec.setSpec(toAcrArtifactTriggerSpec(spec));
        return acrArtifactSpec;
      case ECR:
        EcrArtifactSpec ecrArtifactSpec = new EcrArtifactSpec();
        ecrArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.ECR);
        ecrArtifactSpec.setSpec(toEcrArtifactTriggerSpec(spec));
        return ecrArtifactSpec;
      case BAMBOO:
        BambooArtifactSpec bambooArtifactSpec = new BambooArtifactSpec();
        bambooArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.BAMBOO);
        bambooArtifactSpec.setSpec(toBambooArtifactTriggerSpec(spec));
        return bambooArtifactSpec;
      case JENKINS:
        JenkinsArtifactSpec jenkinsArtifactSpec = new JenkinsArtifactSpec();
        jenkinsArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.JENKINS);
        jenkinsArtifactSpec.setSpec(toJenkinsArtifactTriggerSpec(spec));
        return jenkinsArtifactSpec;
      case GCR:
        GcrArtifactSpec gcrArtifactSpec = new GcrArtifactSpec();
        gcrArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.GCR);
        gcrArtifactSpec.setSpec(toGcrArtifactTriggerSpec(spec));
        return gcrArtifactSpec;
      case AMI:
        AmazonMachineImageArtifactSpec amiArtifactSpec = new AmazonMachineImageArtifactSpec();
        amiArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONMACHINEIMAGE);
        amiArtifactSpec.setSpec(toAmazonMachineImageArtifactTriggerSpec(spec));
        return amiArtifactSpec;
      case AMAZON_S3:
        AmazonS3ArtifactSpec amazonS3ArtifactSpec = new AmazonS3ArtifactSpec();
        amazonS3ArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONS3);
        amazonS3ArtifactSpec.setSpec(toAmazonS3ArtifactTriggerSpec(spec));
        return amazonS3ArtifactSpec;
      case AZURE_ARTIFACTS:
        AzureArtifactsArtifactSpec azureArtifactsArtifactSpec = new AzureArtifactsArtifactSpec();
        azureArtifactsArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONMACHINEIMAGE);
        azureArtifactsArtifactSpec.setSpec(toAzureArtifactsArtifactTriggerSpec(spec));
        return azureArtifactsArtifactSpec;
      case CUSTOM_ARTIFACT:
        CustomArtifactSpec customArtifactSpec = new CustomArtifactSpec();
        customArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.CUSTOMARTIFACT);
        customArtifactSpec.setSpec(toCustomArtifactTriggerSpec(spec));
        return customArtifactSpec;
      case DOCKER_REGISTRY:
        DockerRegistryArtifactSpec dockerRegistryArtifactSpec = new DockerRegistryArtifactSpec();
        dockerRegistryArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.DOCKERREGISTRY);
        dockerRegistryArtifactSpec.setSpec(toDockerRegistryArtifactTriggerSpec(spec));
        return dockerRegistryArtifactSpec;
      case GITHUB_PACKAGES:
        GithubPackageRegistryArtifactSpec githubPackageRegistryArtifactSpec = new GithubPackageRegistryArtifactSpec();
        githubPackageRegistryArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY);
        githubPackageRegistryArtifactSpec.setSpec(toGithubPackageRegistryArtifactTriggerSpec(spec));
        return githubPackageRegistryArtifactSpec;
      case NEXUS2_REGISTRY:
        Nexus2RegistryArtifactSpec nexus2RegistryArtifactSpec = new Nexus2RegistryArtifactSpec();
        nexus2RegistryArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS2REGISTRY);
        nexus2RegistryArtifactSpec.setSpec(toNexus2RegistryArtifactTriggerSpec(spec));
        return nexus2RegistryArtifactSpec;
      case NEXUS3_REGISTRY:
        Nexus3RegistryArtifactSpec nexus3RegistryArtifactSpec = new Nexus3RegistryArtifactSpec();
        nexus3RegistryArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS3REGISTRY);
        nexus3RegistryArtifactSpec.setSpec(toNexus3RegistryArtifactTriggerSpec(spec));
        return nexus3RegistryArtifactSpec;
      case ARTIFACTORY_REGISTRY:
        ArtifactoryRegistryArtifactSpec artifactoryRegistryArtifactSpec = new ArtifactoryRegistryArtifactSpec();
        artifactoryRegistryArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY);
        artifactoryRegistryArtifactSpec.setSpec(toArtifactoryRegistryArtifactTriggerSpec(spec));
        return artifactoryRegistryArtifactSpec;
      case GOOGLE_CLOUD_STORAGE:
        GoogleCloudStorageArtifactSpec googleCloudStorageArtifactSpec = new GoogleCloudStorageArtifactSpec();
        googleCloudStorageArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY);
        googleCloudStorageArtifactSpec.setSpec(toGoogleCloudStorageArtifactTriggerSpec(spec));
        return googleCloudStorageArtifactSpec;
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + spec.fetchBuildType() + " is invalid");
    }
  }

  TriggerConditions toTriggerCondition(TriggerEventDataCondition triggerEventDataCondition) {
    TriggerConditions triggerConditions = new TriggerConditions();
    triggerConditions.setKey(triggerEventDataCondition.getKey());
    triggerConditions.setOperator(toOperatorEnum(triggerEventDataCondition.getOperator()));
    triggerConditions.setValue(triggerEventDataCondition.getValue());
    return triggerConditions;
  }

  TriggerConditions.OperatorEnum toOperatorEnum(ConditionOperator conditionOperator) {
    switch (conditionOperator) {
      case DOES_NOT_CONTAIN:
        return TriggerConditions.OperatorEnum.DOESNOTCONTAIN;
      case CONTAINS:
        return TriggerConditions.OperatorEnum.CONTAINS;
      case REGEX:
        return TriggerConditions.OperatorEnum.REGEX;
      case NOT_IN:
        return TriggerConditions.OperatorEnum.NOTIN;
      case EQUALS:
        return TriggerConditions.OperatorEnum.EQUALS;
      case IN:
        return TriggerConditions.OperatorEnum.IN;
      case ENDS_WITH:
        return TriggerConditions.OperatorEnum.ENDSWITH;
      case NOT_EQUALS:
        return TriggerConditions.OperatorEnum.NOTEQUALS;
      case STARTS_WITH:
        return TriggerConditions.OperatorEnum.STARTSWITH;
      default:
        throw new InvalidRequestException("Conditional Operator " + conditionOperator + " is invalid");
    }
  }
}
