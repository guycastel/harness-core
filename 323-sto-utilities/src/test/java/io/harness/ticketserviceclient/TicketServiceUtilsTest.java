package io.harness.ticketserviceclient;

import com.google.gson.JsonObject;
import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.rule.LifecycleRule;
import io.harness.rule.Owner;
import io.harness.sto.beans.entities.TicketServiceConfig;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static io.harness.rule.OwnerRule.SERGEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TicketServiceUtilsTest extends CategoryTest implements MockableTestMixin {

    @Mock
    private TicketServiceClient ticketServiceClient;
    @Rule
    public LifecycleRule lifecycleRule = new LifecycleRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private final int MAX_RETRY_ATTEMPTS = 3;

    @Test
    @Owner(developers = SERGEY)
    @Category(UnitTests.class)
    public void testGetTicketServiceTokenSuccess() throws Exception {
        String baseUrl = "http://localhost:4444";
        String accountID = "account";
        String globalToken = "token";
        JsonObject ticketServiceTokenResponse = new JsonObject();
        ticketServiceTokenResponse.addProperty("token", "sto-token");
        String stoServiceToken = "sto-token";
        Call<JsonObject> ticketServiceTokenCall = mock(Call.class);
        when(ticketServiceTokenCall.clone()).thenReturn(ticketServiceTokenCall);
        when(ticketServiceTokenCall.execute()).thenReturn(Response.success(ticketServiceTokenResponse));
        when(ticketServiceClient.generateToken(eq(accountID), eq(globalToken))).thenReturn(ticketServiceTokenCall);
        TicketServiceConfig ticketServiceConfig =
                TicketServiceConfig.builder().globalToken(globalToken).baseUrl(baseUrl).internalUrl(baseUrl).build();
        TicketServiceUtils ticketServiceUtils = new TicketServiceUtils(ticketServiceClient, ticketServiceConfig);

        String token = ticketServiceUtils.getTicketServiceToken(accountID);
        assertThat(token).isEqualTo(stoServiceToken);
        verify(ticketServiceTokenCall, times(1)).execute();
        verify(ticketServiceClient, times(1)).generateToken(eq(accountID), eq(globalToken));
    }

    @Test
    @Owner(developers = SERGEY)
    @Category(UnitTests.class)
    public void testGetTicketServiceTokenFailure() throws Exception {
        String baseUrl = "http://localhost:4444";
        String accountID = "account";
        String globalToken = "token";
        Call<JsonObject> ticketServiceTokenCall = mock(Call.class);
        when(ticketServiceTokenCall.clone()).thenReturn(ticketServiceTokenCall);
        when(ticketServiceTokenCall.execute()).thenThrow(new IOException("Got error while trying to process!"));
        when(ticketServiceClient.generateToken(eq(accountID), eq(globalToken))).thenReturn(ticketServiceTokenCall);
        TicketServiceConfig ticketServiceConfig =
                TicketServiceConfig.builder().globalToken(globalToken).baseUrl(baseUrl).internalUrl(baseUrl).build();
        TicketServiceUtils ticketServiceUtils = new TicketServiceUtils(ticketServiceClient, ticketServiceConfig);
        assertThatThrownBy(() -> ticketServiceUtils.getTicketServiceToken(accountID)).isInstanceOf(GeneralException.class);
        verify(ticketServiceTokenCall, times(MAX_RETRY_ATTEMPTS)).execute();
        verify(ticketServiceClient, times(1)).generateToken(eq(accountID), eq(globalToken));
    }

    @Test
    @Owner(developers = SERGEY)
    @Category(UnitTests.class)
    public void testGetTicketServiceTokenUnauthorized() throws Exception {
        String baseUrl = "http://localhost:4444";
        String accountID = "account";
        String globalToken = "token";
        String stoServiceToken = "sto-token";
        Call<JsonObject> ticketServiceTokenCall = mock(Call.class);
        when(ticketServiceTokenCall.clone()).thenReturn(ticketServiceTokenCall);
        ResponseBody errorBody = ResponseBody.create(MediaType.parse("application/json"), "Unauthorized");
        when(ticketServiceTokenCall.execute()).thenReturn(Response.error(401, errorBody));
        when(ticketServiceClient.generateToken(eq(accountID), eq(globalToken))).thenReturn(ticketServiceTokenCall);
        TicketServiceConfig ticketServiceConfig =
                TicketServiceConfig.builder().globalToken(globalToken).baseUrl(baseUrl).internalUrl(baseUrl).build();
        TicketServiceUtils ticketServiceUtils = new TicketServiceUtils(ticketServiceClient, ticketServiceConfig);

        assertThatThrownBy(() -> ticketServiceUtils.getTicketServiceToken(accountID)).isInstanceOf(GeneralException.class);
        verify(ticketServiceTokenCall, times(MAX_RETRY_ATTEMPTS)).execute();
        verify(ticketServiceClient, times(1)).generateToken(eq(accountID), eq(globalToken));
    }
}
