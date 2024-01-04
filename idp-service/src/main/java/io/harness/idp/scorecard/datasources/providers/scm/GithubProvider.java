/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasources.providers.scm;

import static io.harness.idp.common.CommonUtils.parseObjectToString;
import static io.harness.idp.common.Constants.GITHUB_IDENTIFIER;
import static io.harness.idp.common.Constants.GITHUB_INSTALLATION_ID;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.API_BASE_URL;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.AUTHORIZATION_HEADER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.scorecard.datapoints.parser.factory.DataPointParserFactory;
import io.harness.idp.scorecard.datapoints.service.DataPointService;
import io.harness.idp.scorecard.datasourcelocations.locations.DataSourceLocationFactory;
import io.harness.idp.scorecard.datasourcelocations.repositories.DataSourceLocationRepository;
import io.harness.idp.scorecard.datasources.repositories.DataSourceRepository;
import io.harness.idp.scorecard.datasources.utils.ConfigReader;
import io.harness.idp.scorecard.scores.beans.DataFetchDTO;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.IDP)
public class GithubProvider extends ScmBaseProvider {
  private static final String TARGET_URL_EXPRESSION_KEY = "appConfig.integrations.github.0.apiBaseUrl";
  private static final String TOKEN_EXPRESSION_KEY = "appConfig.integrations.github.0.token";
  private static final String GITHUB_APP_ID_EXPRESSION = "appConfig.integrations.github.0.apps.0.appId";
  private static final String GITHUB_APP_PRIVATE_KEY_EXPRESSION = "appConfig.integrations.github.0.apps.0.privateKey";

  final ConfigReader configReader;
  final GithubService githubService;

  public GithubProvider(DataPointService dataPointService, DataSourceLocationFactory dataSourceLocationFactory,
      DataSourceLocationRepository dataSourceLocationRepository, DataPointParserFactory dataPointParserFactory,
      ConfigReader configReader, DataSourceRepository dataSourceRepository, GithubService githubService) {
    super(GITHUB_IDENTIFIER, dataPointService, dataSourceLocationFactory, dataSourceLocationRepository,
        dataPointParserFactory, dataSourceRepository);
    this.configReader = configReader;
    this.githubService = githubService;
  }

  @Override
  public Map<String, Map<String, Object>> fetchData(String accountIdentifier, BackstageCatalogEntity entity,
      List<DataFetchDTO> dataPointsAndInputValues, String configs) {
    Map<String, String> possibleReplaceableUrlBodyPairs = prepareUrlReplaceablePairs(API_BASE_URL,
        parseObjectToString(configReader.getConfigValues(accountIdentifier, configs, TARGET_URL_EXPRESSION_KEY)));
    return scmProcessOut(accountIdentifier, entity, dataPointsAndInputValues, configs, possibleReplaceableUrlBodyPairs);
  }

  @Override
  public Map<String, String> getAuthHeaders(String accountIdentifier, String configs) {
    String token = parseObjectToString(configReader.getConfigValues(accountIdentifier, configs, TOKEN_EXPRESSION_KEY));
    if (StringUtils.isEmpty(token)) {
      String appId =
          parseObjectToString(configReader.getConfigValues(accountIdentifier, configs, GITHUB_APP_ID_EXPRESSION));
      String installationId =
          parseObjectToString(configReader.getDecryptedValue(accountIdentifier, GITHUB_INSTALLATION_ID));
      String privateKey = parseObjectToString(
          configReader.getConfigValues(accountIdentifier, configs, GITHUB_APP_PRIVATE_KEY_EXPRESSION));
      String url =
          parseObjectToString(configReader.getConfigValues(accountIdentifier, configs, TARGET_URL_EXPRESSION_KEY));
      if (!StringUtils.isEmpty(appId) && !StringUtils.isEmpty(installationId) && !StringUtils.isEmpty(privateKey)
          && !StringUtils.isEmpty(url)) {
        token = githubService.getToken(buildGithubAppConfig(appId, installationId, privateKey, url));
      }
    }
    return Map.of(AUTHORIZATION_HEADER, !StringUtils.isEmpty(token) ? "Bearer " + token : StringUtils.EMPTY);
  }

  private GithubAppConfig buildGithubAppConfig(String appId, String installationId, String privateKey, String url) {
    return GithubAppConfig.builder()
        .appId(appId)
        .installationId(installationId)
        .privateKey(privateKey)
        .githubUrl(url)
        .build();
  }
}
