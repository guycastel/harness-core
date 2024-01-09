/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.exception.DuplicateEntityException;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.BaselineRepository;
import io.harness.repositories.SBOMComponentRepo;
import io.harness.spec.server.ssca.v1.model.Artifact;
import io.harness.spec.server.ssca.v1.model.OrchestrationDriftSummary;
import io.harness.spec.server.ssca.v1.model.OrchestrationScorecardSummary;
import io.harness.spec.server.ssca.v1.model.OrchestrationSummaryResponse;
import io.harness.spec.server.ssca.v1.model.SbomDetails;
import io.harness.spec.server.ssca.v1.model.SbomProcessRequestBody;
import io.harness.ssca.beans.SbomDTO;
import io.harness.ssca.beans.SettingsDTO;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.ssca.entities.artifact.ArtifactEntity;
import io.harness.ssca.events.SSCAArtifactCreatedEvent;
import io.harness.ssca.normalize.Normalizer;
import io.harness.ssca.normalize.NormalizerRegistry;
import io.harness.ssca.services.drift.SbomDriftService;
import io.harness.ssca.utils.SBOMUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class OrchestrationStepServiceImpl implements OrchestrationStepService {
  @Inject ArtifactService artifactService;
  @Inject TransactionTemplate transactionTemplate;
  @Inject SBOMComponentRepo SBOMComponentRepo;
  @Inject NormalizerRegistry normalizerRegistry;
  @Inject S3StoreService s3StoreService;
  @Inject BaselineRepository baselineRepository;
  @Inject OutboxService outboxService;

  @Inject SbomDriftService sbomDriftService;

  @Inject @Named("isElasticSearchEnabled") boolean isElasticSearchEnabled;
  private static final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_RETRY_POLICY;

  @Override
  public String processSBOM(String accountId, String orgIdentifier, String projectIdentifier,
      SbomProcessRequestBody sbomProcessRequestBody) throws ParseException {
    // TODO: Check if we can prevent IO Operation.
    // TODO: Use Jackson instead of Gson.
    if (artifactService
            .getArtifact(accountId, orgIdentifier, projectIdentifier,
                sbomProcessRequestBody.getSbomMetadata().getStepExecutionId())
            .isPresent()) {
      throw new DuplicateEntityException(String.format("Artifact already present with orchestration id [%s]",
          sbomProcessRequestBody.getSbomMetadata().getStepExecutionId()));
    }

    log.info("Starting SBOM Processing");
    String sbomFileName = UUID.randomUUID() + "_sbom";
    File sbomDumpFile = new File(sbomFileName);
    try (FileOutputStream fos = new FileOutputStream(sbomFileName)) {
      byte[] data = sbomProcessRequestBody.getSbomProcess().getData();
      fos.write(data);
    } catch (IOException e) {
      log.error(String.format("Error in writing sbom to file: %s", sbomDumpFile));
    }

    ArtifactEntity artifactEntity;
    SbomDTO sbomDTO = SBOMUtils.getSbomDTO(
        sbomProcessRequestBody.getSbomProcess().getData(), sbomProcessRequestBody.getSbomMetadata().getFormat());
    artifactEntity = artifactService.getArtifactFromSbomPayload(
        accountId, orgIdentifier, projectIdentifier, sbomProcessRequestBody, sbomDTO);

    uploadSbomAndDeleteLocalFile(sbomDumpFile, artifactEntity);

    SettingsDTO settingsDTO =
        getSettingsDTO(accountId, orgIdentifier, projectIdentifier, sbomProcessRequestBody, artifactEntity);

    Normalizer normalizer = normalizerRegistry.getNormalizer(settingsDTO.getFormat()).get();
    List<NormalizedSBOMComponentEntity> sbomEntityList = normalizer.normaliseSBOM(sbomDTO, settingsDTO);

    artifactEntity.setComponentsCount(sbomEntityList.stream().count());

    Boolean baselineExists =
        baselineRepository.existsByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndArtifactIdAndTag(
            accountId, orgIdentifier, projectIdentifier, artifactEntity.getArtifactId(), artifactEntity.getTag());

    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      artifactService.saveArtifactAndInvalidateOldArtifact(artifactEntity);
      SBOMComponentRepo.saveAll(sbomEntityList);
      if (isElasticSearchEnabled) {
        outboxService.save(new SSCAArtifactCreatedEvent(accountId, orgIdentifier, projectIdentifier, artifactEntity));
      }
      if (baselineExists) {
        baselineRepository.updateOrchestrationId(accountId, orgIdentifier, projectIdentifier,
            artifactEntity.getArtifactId(), artifactEntity.getTag(), artifactEntity.getOrchestrationId());
      }
      log.info(String.format("SBOM Processed Successfully, Artifact ID: %s", artifactEntity.getArtifactId()));
      return artifactEntity.getArtifactId();
    }));
  }

  private void uploadSbomAndDeleteLocalFile(File sbomDumpFile, ArtifactEntity artifactEntity) {
    try {
      s3StoreService.uploadSBOM(sbomDumpFile, artifactEntity);
    } finally {
      sbomDumpFile.delete();
    }
  }

  @Override
  public OrchestrationSummaryResponse getOrchestrationSummary(
      String accountId, String orgIdentifier, String projectIdentifier, String orchestrationId) {
    ArtifactEntity artifact =
        artifactService.getArtifact(accountId, orgIdentifier, projectIdentifier, orchestrationId)
            .orElseThrow(()
                             -> new NotFoundException(String.format(
                                 "Artifact with orchestrationIdentifier [%s] is not found", orchestrationId)));

    Artifact artifactResponse = new Artifact()
                                    .name(artifact.getName())
                                    .type(Artifact.TypeEnum.fromValue(artifact.getType()))
                                    .registryUrl(artifact.getUrl())
                                    .id(artifact.getId())
                                    .tag(artifact.getTag());

    OrchestrationScorecardSummary scorecardSummary = null;

    if (artifact.getScorecard() != null) {
      scorecardSummary = new OrchestrationScorecardSummary()
                             .avgScore(artifact.getScorecard().getAvgScore())
                             .maxScore(artifact.getScorecard().getMaxScore());
    }

    OrchestrationDriftSummary driftSummary =
        sbomDriftService.getSbomDriftSummary(accountId, orgIdentifier, projectIdentifier, orchestrationId);

    return new OrchestrationSummaryResponse()
        .artifact(artifactResponse)
        .stepExecutionId(artifact.getOrchestrationId())
        .isAttested(artifact.isAttested())
        .sbom(new SbomDetails().name(artifact.getSbomName()))
        .driftSummary(driftSummary)
        .scorecardSummary(scorecardSummary);
  }

  @Override
  public String sbom(String accountId, String orgIdentifier, String projectIdentifier, String orchestrationId) {
    ArtifactEntity artifact =
        artifactService.getArtifact(accountId, orgIdentifier, projectIdentifier, orchestrationId)
            .orElseThrow(()
                             -> new NotFoundException(String.format(
                                 "Artifact with orchestrationIdentifier [%s] is not found", orchestrationId)));

    File sbomFile = s3StoreService.downloadSBOM(artifact);

    try {
      String fileContent = Files.readString(Paths.get(sbomFile.getPath()), StandardCharsets.UTF_8);
      sbomFile.delete();
      return fileContent;
    } catch (IOException e) {
      throw new RuntimeException("Error reading the SBOM file");
    }
  }

  private SettingsDTO getSettingsDTO(String accountId, String orgIdentifier, String projectIdentifier,
      SbomProcessRequestBody sbomProcessRequestBody, ArtifactEntity artifactEntity) {
    return SettingsDTO.builder()
        .orchestrationID(sbomProcessRequestBody.getSbomMetadata().getStepExecutionId())
        .pipelineIdentifier(sbomProcessRequestBody.getSbomMetadata().getPipelineIdentifier())
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .sequenceID(sbomProcessRequestBody.getSbomMetadata().getSequenceId())
        .accountID(accountId)
        .artifactURL(sbomProcessRequestBody.getArtifact().getRegistryUrl())
        .artifactID(artifactEntity.getArtifactId())
        .artifactTag(artifactEntity.getTag())
        .format(sbomProcessRequestBody.getSbomMetadata().getFormat())
        .tool(
            SettingsDTO.Tool.builder().name(sbomProcessRequestBody.getSbomMetadata().getTool()).version("2.0").build())
        .build();
  }
}
