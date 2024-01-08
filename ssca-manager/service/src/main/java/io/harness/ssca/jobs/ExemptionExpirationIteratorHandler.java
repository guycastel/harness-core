/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.jobs;

import static io.harness.annotations.dev.HarnessTeam.SSCA;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.SSCAIteratorConfig;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ssca.entities.exemption.Exemption;
import io.harness.ssca.entities.exemption.Exemption.ExemptionKeys;
import io.harness.ssca.entities.exemption.Exemption.ExemptionStatus;
import io.harness.ssca.services.exemption.ExemptionService;

import com.google.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(SSCA)
@Slf4j
public class ExemptionExpirationIteratorHandler implements MongoPersistenceIterator.Handler<Exemption> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  @Inject private MongoTemplate mongoTemplate;

  @Inject private ExemptionService exemptionService;

  public void registerIterators(SSCAIteratorConfig config) {
    if (config == null || !config.isEnabled()) {
      return;
    }
    SpringFilterExpander filterExpander = getFilterQuery();
    registerIteratorWithFactory(config, filterExpander);
  }

  private void registerIteratorWithFactory(SSCAIteratorConfig config, @NotNull SpringFilterExpander filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(config.getThreadPoolSize())
            .interval(ofSeconds(1))
            .build(),
        Exemption.class,
        MongoPersistenceIterator.<Exemption, SpringFilterExpander>builder()
            .clazz(Exemption.class)
            .fieldName(ExemptionKeys.iteration)
            .acceptableNoAlertDelay(ofMinutes(5))
            .targetInterval(ofSeconds(config.getTargetIntervalInSeconds()))
            .filterExpander(filterExpander)
            .acceptableExecutionTime(ofSeconds(30))
            .acceptableNoAlertDelay(ofMinutes(5))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  private SpringFilterExpander getFilterQuery() {
    return query -> {
      Criteria criteria = Criteria.where(ExemptionKeys.exemptionStatus)
                              .is(ExemptionStatus.APPROVED)
                              .and(ExemptionKeys.validUntil)
                              .lt(System.currentTimeMillis());

      query.addCriteria(criteria);
    };
  }

  @Override
  public void handle(Exemption exemption) {
    try {
      exemptionService.expireExemption(exemption.getAccountId(), exemption.getOrgIdentifier(),
          exemption.getProjectIdentifier(), exemption.getArtifactId(), exemption.getUuid());
    } catch (NotFoundException exception) {
      log.warn("Exemption {} for accountId {} orgIdentifier {} projectIdentifier {} artifactId {} not found.",
          exemption.getUuid(), exemption.getAccountId(), exemption.getOrgIdentifier(), exemption.getProjectIdentifier(),
          exemption.getArtifactId());
    } catch (BadRequestException exception) {
      log.warn("Exemption {} for accountId {} orgIdentifier {} projectIdentifier {} artifactId {} can not be expired.",
          exemption.getUuid(), exemption.getAccountId(), exemption.getOrgIdentifier(), exemption.getProjectIdentifier(),
          exemption.getArtifactId());
    }
  }
}
