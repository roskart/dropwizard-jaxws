package com.roskart.dropwizard.jaxws;

import com.roskart.dropwizard.jaxws.auth.BasicAuthenticator;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.Principal;

import static com.roskart.dropwizard.jaxws.BasicAuthenticationInterceptor.PRINCIPAL_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BasicAuthenticationInterceptorTest {

    @Mock
    private InterceptorChain interceptorChainMock;
    @Mock
    private Destination destinationMock;
    @Mock
    private Conduit conduitMock;
    @Mock
    private Message inMessageMock;
    @Mock
    private Message outMessageMock;
    @Mock
    private OutputStream outputStreamMock;

    private AutoCloseable closeable;

    private final BasicAuthentication basicAuthentication = new BasicAuthentication(new BasicAuthenticator(), "TOP_SECRET");
    // Suppress warning about "hard-coded" password
    @SuppressWarnings("squid:S2068")
    private static final String CORRECT_PASSWORD = "secret";
    private static final String USERNAME = "username";

    @BeforeEach
    public void setup() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        when(destinationMock.getBackChannel(any())).thenReturn(conduitMock);
        when(outMessageMock.getContent(OutputStream.class)).thenReturn(outputStreamMock);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    void shouldAuthenticateValidUser() {
        BasicAuthenticationInterceptor target = new BasicAuthenticationInterceptor();
        target.setAuthenticator(basicAuthentication);
        Message message = createMessageWithUsernameAndPassword(USERNAME, CORRECT_PASSWORD);

        target.handleMessage(message);

        verify(inMessageMock).put(eq(PRINCIPAL_KEY), any(Principal.class));
    }

    @Test
    void shouldReturnUnathorizedCodeForInvalidCredentials() {
        BasicAuthenticationInterceptor target = new BasicAuthenticationInterceptor();
        target.setAuthenticator(basicAuthentication);
        Message message = createMessageWithUsernameAndPassword(USERNAME, "foo");

        target.handleMessage(message);

        verify(outMessageMock).put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    void shouldNotCrashOnNullPassword() {
        BasicAuthenticationInterceptor target = new BasicAuthenticationInterceptor();
        target.setAuthenticator(basicAuthentication);
        Message message = createMessageWithUsernameAndPassword(USERNAME, null);

        target.handleMessage(message);

        verify(outMessageMock).put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    void shouldNotCrashOnNullUser() {
        BasicAuthenticationInterceptor target = new BasicAuthenticationInterceptor();
        target.setAuthenticator(basicAuthentication);
        Message message = createMessageWithUsernameAndPassword(null, CORRECT_PASSWORD);

        target.handleMessage(message);

        verify(outMessageMock).put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    private Message createMessageWithUsernameAndPassword(String username, String password) {
        Message message = createEmptyMessage();

        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setUserName(username);
        policy.setPassword(password);
        message.put(AuthorizationPolicy.class, policy);
        return message;
    }

    private Message createEmptyMessage() {
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(inMessageMock);
        exchange.setOutMessage(outMessageMock);
        exchange.setDestination(destinationMock);

        Message message = new MessageImpl();
        message.setExchange(exchange);
        message.setInterceptorChain(interceptorChainMock);
        return message;
    }
}
