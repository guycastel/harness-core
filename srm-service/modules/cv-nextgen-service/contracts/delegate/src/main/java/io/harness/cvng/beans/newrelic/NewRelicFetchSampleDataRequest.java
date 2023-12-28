/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans.newrelic;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.cvng.newrelic.NewRelicUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("NEWRELIC_SAMPLE_FETCH_REQUEST")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class NewRelicFetchSampleDataRequest extends DataCollectionRequest<NewRelicConnectorDTO> {
  public static final String DSL =
      DataCollectionRequest.readDSL("newrelic-sample-fetch.datacollection", NewRelicFetchSampleDataRequest.class);
  public static final String INSIGHTS_DSL =
      DataCollectionRequest.readDSL("newrelic-sample-fetch.datacollection", NewRelicMetricPackValidationRequest.class);
  public static final String NERDGRAPHQL_API_DSL = DataCollectionRequest.readDSL(
      "newrelic-api-sample-fetch.datacollection", NewRelicMetricPackValidationRequest.class);
  public static final String INSIGHTS = "insights";
  public static final String GRAPHQL_ENDPOINT = "https://api.newrelic.com/graphql";

  String query;
  @Override
  public String getDSL() {
    if (getConnectorConfigDTO().getUrl().contains(INSIGHTS)) {
      return INSIGHTS_DSL;
    }
    return NERDGRAPHQL_API_DSL;
  }

  @Override
  public String getBaseUrl() {
    return NewRelicUtils.getBaseUrl(getConnectorConfigDTO());
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return NewRelicUtils.collectionHeaders(getConnectorConfigDTO());
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("query", query);
    variables.put("accountId", getConnectorConfigDTO().getNewRelicAccountId());
    variables.put("graphqlURL", GRAPHQL_ENDPOINT);
    return variables;
  }
}
