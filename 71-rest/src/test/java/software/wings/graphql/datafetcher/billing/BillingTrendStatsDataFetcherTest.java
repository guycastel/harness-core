package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingTrendStats;
import software.wings.security.UserThreadLocal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BillingTrendStatsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock private DataFetcherUtils utils;
  @Mock TimeScaleDBService timeScaleDBService;
  @Inject @InjectMocks BillingTrendStatsDataFetcher billingTrendStatsDataFetcher;

  @Mock ResultSet resultSet;
  @Mock ResultSet trendResultSet;

  private final BigDecimal TOTAL_COST = new BigDecimal("10.660");
  private final BigDecimal TOTAL_TREND_COST = new BigDecimal(5);
  private Instant END_TIME = Instant.ofEpochMilli(1571509800000l);
  private Instant START_TIME = Instant.ofEpochMilli(1570645800000l);
  private Instant TREND_END_TIME = Instant.ofEpochMilli(1570473000000l);
  private Instant TREND_START_TIME = Instant.ofEpochMilli(1568226600000l);
  final int[] count = {0};
  final int[] countTrend = {0};

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);

    mockResultSet();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingTrend() throws SQLException {
    prepareDataForTrend();

    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLBillingDataFilter> filters = createFilter();
    QLBillingTrendStats data = (QLBillingTrendStats) billingTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getTotalCost().getStatsValue()).isEqualTo("$10.66");
    assertThat(data.getTotalCost().getStatsDescription()).isEqualTo("of Oct 09 - Oct 19");
    assertThat(data.getCostTrend().getStatsValue()).isEqualTo("113.2");
    assertThat(data.getCostTrend().getStatsDescription()).isEqualTo("$5.66 over Sep 11 - Oct 09");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingTrendWithForecast() throws SQLException {
    prepareDataForTrend();

    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLBillingDataFilter> filters = createForecastFilter();
    QLBillingTrendStats data = (QLBillingTrendStats) billingTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getTotalCost().getStatsValue()).isEqualTo("$10.66");
    assertThat(data.getTotalCost().getStatsDescription()).isEqualTo("of Oct 09 - Oct 19");
    assertThat(data.getCostTrend().getStatsValue()).isEqualTo("NA");
    assertThat(data.getCostTrend().getStatsDescription()).isNotEqualTo("-");
    assertThat(data.getForecastCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getForecastCost().getStatsDescription()).isEqualTo("-");
    assertThat(data.getEfficiencyScore().getStatsValue()).isEqualTo("-1");
  }

  private void prepareDataForTrend() throws SQLException {
    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString()))
        .thenReturn(resetCountAndReturnResultSet())
        .thenReturn(resetCountAndReturnTrendResultSet());
    when(timeScaleDBService.isValid()).thenReturn(true);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetEmptyBillingTrend() throws SQLException {
    Statement mockStatement = mock(Statement.class);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(null);
    when(timeScaleDBService.isValid()).thenReturn(true);

    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    QLBillingTrendStats data = (QLBillingTrendStats) billingTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data.getTotalCost()).isNull();
    assertThat(data.getForecastCost()).isNull();
    assertThat(data.getCostTrend()).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    assertThatThrownBy(()
                           -> billingTrendStatsDataFetcher.fetch(ACCOUNT1_ID, aggregationFunction,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenQueryThrowsException() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    Statement mockStatement = mock(Statement.class);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException());

    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    QLBillingTrendStats data = (QLBillingTrendStats) billingTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data.getTotalCost()).isNull();
  }

  private List<QLBillingDataFilter> createFilter() {
    String[] appIdFilterValues = new String[] {""};
    return Arrays.asList(
        makeApplicationFilter(appIdFilterValues), startTimeFilter(START_TIME), endTimeFilter(END_TIME));
  }

  private List<QLBillingDataFilter> createForecastFilter() {
    String[] clusterIdFilterValues = new String[] {""};
    List<QLBillingDataFilter> forecastFilter = new ArrayList<>();
    forecastFilter.add(makeClusterFilter(clusterIdFilterValues));
    forecastFilter.add(startTimeFilter(START_TIME));
    forecastFilter.add(endTimeFilter(Instant.now().plus(5, ChronoUnit.DAYS)));
    return forecastFilter;
  }

  public QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  public QLBillingDataFilter makeApplicationFilter(String[] values) {
    QLIdFilter applicationFilter = QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build();
    return QLBillingDataFilter.builder().application(applicationFilter).build();
  }

  private QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  public QLBillingDataFilter startTimeFilter(Instant instant) {
    QLTimeFilter timeFilter =
        QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(instant.toEpochMilli()).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  public QLBillingDataFilter endTimeFilter(Instant instant) {
    QLTimeFilter timeFilter =
        QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(instant.toEpochMilli()).build();
    return QLBillingDataFilter.builder().endTime(timeFilter).build();
  }

  private void mockResultSet() throws SQLException {
    resultSet = mock(ResultSet.class);
    trendResultSet = mock(ResultSet.class);

    when(resultSet.getBigDecimal(anyString())).thenReturn(TOTAL_COST);

    when(resultSet.getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(START_TIME.toEpochMilli()));
    when(resultSet.getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(END_TIME.toEpochMilli()));

    when(trendResultSet.getBigDecimal(anyString())).thenReturn(TOTAL_TREND_COST);

    when(
        trendResultSet.getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(TREND_START_TIME.toEpochMilli()));
    when(
        trendResultSet.getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(TREND_END_TIME.toEpochMilli()));

    returnResultSet(1);
  }

  private void returnResultSet(int limit) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      count[0] = 0;
      return false;
    });
    when(trendResultSet.next()).then((Answer<Boolean>) invocation -> {
      if (countTrend[0] < limit) {
        countTrend[0]++;
        return true;
      }
      countTrend[0] = 0;
      return false;
    });
  }

  private ResultSet resetCountAndReturnResultSet() {
    count[0] = 0;
    return resultSet;
  }

  private ResultSet resetCountAndReturnTrendResultSet() {
    countTrend[0] = 0;
    return trendResultSet;
  }
}
