package com.roskart.dropwizard.jaxws;

import ch.qos.logback.classic.Level;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;

public class JAXWSEnvironmentTest {

    private JAXWSEnvironment jaxwsEnvironment;
    private Invoker mockInvoker = mock(Invoker.class);
    private TestUtilities testutils = new TestUtilities(JAXWSEnvironmentTest.class);
    private DummyService service = new DummyService();
    InstrumentedInvokerFactory mockInvokerBuilder = mock(InstrumentedInvokerFactory.class);
    UnitOfWorkInvokerFactory mockUnitOfWorkInvokerBuilder = mock(UnitOfWorkInvokerFactory.class);
    private int mockBasicAuthInterceptorInvoked;

    private String soapRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:res=\"http://jaxws.dropwizard.roskart.com/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                    "<res:foo></res:foo>" +
                "</soapenv:Body>" +
            "</soapenv:Envelope>";

    // DummyInterface is used by getClient test
    @WebService
    interface DummyInterface {
        @WebMethod
        public void foo();
    }

    @Before
    public void setup() {

        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("org.apache.cxf")).setLevel(Level.INFO);

        jaxwsEnvironment = new JAXWSEnvironment("soap") {
            /*
            We create BasicAuthenticationInterceptor mock manually, because Mockito provided mock
            does not get invoked by CXF
            */
            @Override
            protected BasicAuthenticationInterceptor createBasicAuthenticationInterceptor() {
                return new BasicAuthenticationInterceptor() {
                    @Override
                    public void handleMessage(Message message) throws Fault {
                        mockBasicAuthInterceptorInvoked++;
                    }
                };
            }
        };

        when(mockInvokerBuilder.create(any(), any(Invoker.class))).thenReturn(mockInvoker);
        jaxwsEnvironment.setInstrumentedInvokerBuilder(mockInvokerBuilder);

        when(mockUnitOfWorkInvokerBuilder
                .create(any(), any(Invoker.class), any(SessionFactory.class)))
                .thenReturn(mockInvoker);
        jaxwsEnvironment.setUnitOfWorkInvokerBuilder(mockUnitOfWorkInvokerBuilder);

        mockBasicAuthInterceptorInvoked = 0;

        testutils.setBus(jaxwsEnvironment.bus);
        testutils.addNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        testutils.addNamespace("a", "http://jaxws.dropwizard.roskart.com/");
    }

    @After
    public void teardown() {
        jaxwsEnvironment.bus.shutdown(false);
    }

    @Test
    public void buildServlet() {
        Object result = jaxwsEnvironment.buildServlet();
        assertThat(result, is(instanceOf(CXFNonSpringServlet.class)));
        assertThat(((CXFNonSpringServlet) result).getBus(), is(instanceOf(Bus.class)));
    }

    @Test
    public void publishEndpoint() throws Exception {

        jaxwsEnvironment.publishEndpoint("local://path", service);

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyZeroInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

    }

    @Test
    public void publishEndpointWithAuthentication() throws Exception {

        jaxwsEnvironment.publishEndpoint("local://path", service, mock(BasicAuthentication.class));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyZeroInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        assertEquals(1, mockBasicAuthInterceptorInvoked);
    }

    @Test
    public void publishEndpointWithHibernateInvoker() throws Exception {

        jaxwsEnvironment.publishEndpoint("local://path", service, mock(SessionFactory.class));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verify(mockUnitOfWorkInvokerBuilder).create(any(), any(Invoker.class), any(SessionFactory.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    public void publishEndpointWithInvalidArguments() throws Exception {

        try {
            jaxwsEnvironment.publishEndpoint("foo", null);
            fail();
        }

        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsEnvironment.publishEndpoint(null, service);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsEnvironment.publishEndpoint("   ", service);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    public void getClient() {
        String address = "http://address";
        Handler handler = mock(Handler.class);

        DummyInterface clientProxy = jaxwsEnvironment.getClient(DummyInterface.class, address);
        assertThat(clientProxy, is(instanceOf(Proxy.class)));

        Client c = ClientProxy.getClient(clientProxy);
        assertEquals(address, c.getEndpoint().getEndpointInfo().getAddress());
        assertEquals(DummyInterface.class, (c.getEndpoint().getService().get("endpoint.class")));
        assertEquals(0, ((BindingProvider)clientProxy).getBinding().getHandlerChain().size());

        HTTPClientPolicy httpclient = ((HTTPConduit)c.getConduit()).getClient();
        assertEquals(500, httpclient.getConnectionTimeout());
        assertEquals(2000, httpclient.getReceiveTimeout());

        // with handler
        clientProxy = jaxwsEnvironment.getClient(DummyInterface.class, address, handler);

        assertEquals(1, ((BindingProvider)clientProxy).getBinding().getHandlerChain().size());
        assertEquals(handler, ((BindingProvider)clientProxy).getBinding().getHandlerChain().get(0));
    }

}
