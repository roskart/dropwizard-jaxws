package com.roskart.dropwizard.jaxws;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.TokenType;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A CXF interceptor that manages HTTP Basic Authentication. Implementation is based on combination of
 * CXF JAASLoginInterceptor code and the following Github Gist: https://gist.github.com/palesz/3438143.
 * Dropwizard authenticator is used for credentials authentication. Authenticated principal is stored in message
 * exchange and is available in the service implementation through JAX-WS WebServiceContext.
 */
public class BasicAuthenticationInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthenticationInterceptor.class);
    public static final String PRINCIPAL_KEY = "dropwizard.jaxws.principal";
    private BasicAuthentication authentication;

    public BasicAuthenticationInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void setAuthenticator(BasicAuthentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public void handleMessage(final Message message) throws Fault {

        final Exchange exchange = message.getExchange();

        BasicCredentials credentials = null;

        try {
            AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
            if (policy != null) {
                credentials = new BasicCredentials(policy.getUserName(), policy.getPassword());
            } else {
                // try the WS-Security UsernameToken
                SecurityToken token = message.get(SecurityToken.class);
                if (token != null && token.getTokenType() == TokenType.UsernameToken) {
                    UsernameToken ut = (UsernameToken)token;
                    credentials = new BasicCredentials(ut.getName(), ut.getPassword());
                }
            }

            if (credentials == null) {
                sendErrorResponse(message, HttpURLConnection.HTTP_UNAUTHORIZED);
                return;
            }

            Optional<?> principal = authentication.getAuthenticator().authenticate(
                    new BasicCredentials(credentials.getUsername(), credentials.getPassword()));

            // principal will be available through JAX-WS WebServiceContext
            if (principal.isPresent()) {
                exchange.getInMessage().put(PRINCIPAL_KEY, principal.get());
            }
        }
        catch (AuthenticationException ae) {
            sendErrorResponse(message, HttpURLConnection.HTTP_FORBIDDEN);
            return;
        }
    }

    private void sendErrorResponse(Message message, int responseCode) {
        Message outMessage = getOutMessage(message);
        outMessage.put(Message.RESPONSE_CODE, responseCode);
        // Set the response headers
        @SuppressWarnings("unchecked")
        Map<String, List<String>> responseHeaders = (Map)message.get(Message.PROTOCOL_HEADERS);
        if (responseHeaders != null) {
            responseHeaders.put("WWW-Authenticate", Arrays.asList("Basic realm=" + authentication.getRealm()));
            responseHeaders.put("Content-length", Arrays.<String>asList("0"));
        }
        message.getInterceptorChain().abort();
        try {
            getConduit(message).prepare(outMessage);
            close(outMessage);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private Message getOutMessage(Message inMessage) {
        Exchange exchange = inMessage.getExchange();
        Message outMessage = exchange.getOutMessage();
        if (outMessage == null) {
            Endpoint endpoint = exchange.get(Endpoint.class);
            outMessage = endpoint.getBinding().createMessage();
            exchange.setOutMessage(outMessage);
        }
        outMessage.putAll(inMessage);
        return outMessage;
    }

    private Conduit getConduit(Message inMessage) throws IOException {
        Exchange exchange = inMessage.getExchange();
        EndpointReferenceType target = exchange.get(EndpointReferenceType.class);
        Conduit conduit = exchange.getDestination().getBackChannel(inMessage, null, target);
        exchange.setConduit(conduit);
        return conduit;
    }

    private void close(Message outMessage) throws IOException {
        OutputStream os = outMessage.getContent(OutputStream.class);
        os.flush();
        os.close();
    }
}
