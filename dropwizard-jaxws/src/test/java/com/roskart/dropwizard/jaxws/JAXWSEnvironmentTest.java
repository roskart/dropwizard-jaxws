package com.roskart.dropwizard.jaxws;

import ch.qos.logback.classic.Level;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
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
import javax.xml.ws.handler.MessageContext;

import java.lang.reflect.Proxy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.*;

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

    // TestInterceptor is used for testing CXF interceptors
    class TestInterceptor extends AbstractPhaseInterceptor<Message> {
        private int invocationCount = 0;
        public TestInterceptor(String phase) {
            super(phase);
        }
        public int getInvocationCount() {
            return this.invocationCount;
        }
        @Override
        public void handleMessage(Message message) throws Fault {
            invocationCount++;
        }
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

        jaxwsEnvironment.publishEndpoint(new EndpointBuilder("local://path", service));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyZeroInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    public void publishEndpointWithAuthentication() throws Exception {

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                    .authentication(mock(BasicAuthentication.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verifyZeroInteractions(mockUnitOfWorkInvokerBuilder);

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        assertThat(mockBasicAuthInterceptorInvoked, equalTo(1));
    }

    @Test
    public void publishEndpointWithHibernateInvoker() throws Exception {

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                    .sessionFactory(mock(SessionFactory.class)));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));
        verify(mockUnitOfWorkInvokerBuilder).create(any(), any(Invoker.class), any(SessionFactory.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    public void publishEndpointWithCxfInterceptors() throws Exception {

        TestInterceptor inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        TestInterceptor inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        TestInterceptor outInterceptor = new TestInterceptor(Phase.MARSHAL);

        jaxwsEnvironment.publishEndpoint(
                new EndpointBuilder("local://path", service)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor));

        verify(mockInvokerBuilder).create(any(), any(Invoker.class));

        Node soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker).invoke(any(Exchange.class), any());
        assertThat(inInterceptor.getInvocationCount(), equalTo(1));
        assertThat(inInterceptor2.getInvocationCount(), equalTo(1));
        assertThat(outInterceptor.getInvocationCount(), equalTo(1));

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);

        soapResponse = testutils.invoke("local://path",
                LocalTransportFactory.TRANSPORT_ID, soapRequest.getBytes());

        verify(mockInvoker, times(2)).invoke(any(Exchange.class), any());
        assertThat(inInterceptor.getInvocationCount(), equalTo(2));
        assertThat(inInterceptor2.getInvocationCount(), equalTo(2));
        assertThat(outInterceptor.getInvocationCount(), equalTo(2));

        testutils.assertValid("/soap:Envelope/soap:Body/a:fooResponse", soapResponse);
    }

    @Test
    public void publishEndpointWithInvalidArguments() throws Exception {

        try {
            jaxwsEnvironment.publishEndpoint(new EndpointBuilder("foo", null));
        }
        catch (IllegalArgumentException e) {
        }

        try {
            jaxwsEnvironment.publishEndpoint(new EndpointBuilder(null, service));
        }
        catch (IllegalArgumentException e) {
        }

        try {
            jaxwsEnvironment.publishEndpoint(new EndpointBuilder("   ", service));
        }
        catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void getClient() {
        String address = "http://address";
        Handler handler = mock(Handler.class);

        // simple
        DummyInterface clientProxy = jaxwsEnvironment.getClient(
                new ClientBuilder<DummyInterface>(DummyInterface.class, address)
        );
        assertThat(clientProxy, is(instanceOf(Proxy.class)));

        Client c = ClientProxy.getClient(clientProxy);
        assertThat(c.getEndpoint().getEndpointInfo().getAddress(), equalTo(address));
        assertThat(c.getEndpoint().getService().get("endpoint.class").equals(DummyInterface.class), equalTo(true));
        assertThat(((BindingProvider)clientProxy).getBinding().getHandlerChain().size(), equalTo(0));

        HTTPClientPolicy httpclient = ((HTTPConduit)c.getConduit()).getClient();
        assertThat(httpclient.getConnectionTimeout(), equalTo(500L));
        assertThat(httpclient.getReceiveTimeout(), equalTo(2000L));

        // with timeouts, handlers and interceptors

        TestInterceptor inInterceptor = new TestInterceptor(Phase.UNMARSHAL);
        TestInterceptor inInterceptor2 = new TestInterceptor(Phase.PRE_INVOKE);
        TestInterceptor outInterceptor = new TestInterceptor(Phase.MARSHAL);

        clientProxy = jaxwsEnvironment.getClient(
                new ClientBuilder<DummyInterface>(DummyInterface.class, address)
                        .connectTimeout(123)
                        .receiveTimeout(456)
                        .handlers(handler)
                        .cxfInInterceptors(inInterceptor, inInterceptor2)
                        .cxfOutInterceptors(outInterceptor));
        c = ClientProxy.getClient(clientProxy);
        assertThat(c.getEndpoint().getEndpointInfo().getAddress(), equalTo(address));
        assertThat(c.getEndpoint().getService().get("endpoint.class").equals(DummyInterface.class), equalTo(true));

        httpclient = ((HTTPConduit)c.getConduit()).getClient();
        assertThat(httpclient.getConnectionTimeout(), equalTo(123L));
        assertThat(httpclient.getReceiveTimeout(), equalTo(456L));

        assertThat(((BindingProvider)clientProxy).getBinding().getHandlerChain(), contains(handler));
    }
}
