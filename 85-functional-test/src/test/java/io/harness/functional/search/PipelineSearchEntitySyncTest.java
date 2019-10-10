package io.harness.functional.search;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.SearchRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.Pipeline;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

@Slf4j
public class PipelineSearchEntitySyncTest extends AbstractFunctionalTest {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;
  private static final Retry retry = new Retry(10, 5000);
  private final String APP_NAME = "SyncTestApplication" + System.currentTimeMillis();
  private final String PIPELINE_NAME = "SyncTestPipeline" + System.currentTimeMillis();
  private final String EDITED_PIPELINE_NAME = PIPELINE_NAME + "_Edited";
  private Application application;
  private Pipeline pipeline;

  @Before
  public void setUp() {
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
      return;
    }

    application = new Application();
    application.setAccountId(getAccount().getUuid());
    application.setName(APP_NAME);

    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(application).isNotNull();

    pipeline = new Pipeline();
    pipeline.setAppId(application.getUuid());
    pipeline.setName(PIPELINE_NAME);

    pipeline = PipelineRestUtils.createPipeline(pipeline.getAppId(), pipeline, getAccount().getUuid(), bearerToken);
    assertThat(pipeline).isNotNull();
  }

  @Test
  @Owner(emails = UTKARSH)
  @Category(FunctionalTests.class)
  public void testPipelineCRUDSync() {
    if (!featureFlagService.isGlobalEnabled(FeatureName.SEARCH) || !mainConfiguration.isSearchEnabled()) {
      return;
    }

    BooleanMatcher booleanMatcher = new BooleanMatcher();
    retry.executeWithRetry(this ::isPipelineInSearchResponse, booleanMatcher, true);
    logger.info("New pipeline with id {} and name {} synced.", pipeline.getUuid(), pipeline.getName());

    pipeline.setName(EDITED_PIPELINE_NAME);
    pipeline = PipelineRestUtils.updatePipeline(application.getUuid(), pipeline, bearerToken);

    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getName()).isEqualTo(EDITED_PIPELINE_NAME);

    retry.executeWithRetry(this ::isPipelineInSearchResponse, booleanMatcher, true);
    logger.info("Pipeline update with id {} and name {} synced.", pipeline.getUuid(), pipeline.getName());

    int statusCode = PipelineRestUtils.deletePipeline(pipeline.getAppId(), pipeline.getUuid(), bearerToken);
    assertThat(statusCode).isEqualTo(HttpStatus.SC_OK);

    retry.executeWithRetry(this ::isPipelineInSearchResponse, booleanMatcher, false);
    logger.info("Pipeline with id {} deleted", pipeline.getUuid());

    ApplicationRestUtils.deleteApplication(bearerToken, application.getUuid(), application.getAccountId());
  }

  private boolean isPipelineInSearchResponse() {
    boolean pipelineFound = false;

    SearchResponse searchResponse = SearchRestUtils.search(bearerToken, application.getAccountId(), pipeline.getName());

    List<PipelineView> pipelineViews = searchResponse.getPipelines();

    for (PipelineView pipelineView : pipelineViews) {
      if (pipelineView.getId().equals(pipeline.getUuid()) && pipelineView.getName().equals(pipeline.getName())
          && pipelineView.getAppId().equals(pipeline.getAppId())
          && pipelineView.getAppName().equals(application.getName())) {
        pipelineFound = true;
        break;
      }
    }
    return pipelineFound;
  }
}