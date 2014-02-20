package com.roskart.dropwizard.jaxws;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.xml.ws.handler.Handler;

import java.util.EventListener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JAXWSBundleTest {

    Environment environment = mock(Environment.class);
    Bootstrap bootstrap = mock(Bootstrap.class);
    ServletEnvironment servletEnvironment = mock(ServletEnvironment.class);
    ServletRegistration.Dynamic servlet = mock(ServletRegistration.Dynamic.class);
    JAXWSEnvironment jaxwsEnvironment = mock(JAXWSEnvironment.class);
    LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);

    @Before
    public void setUp() {
        when(environment.servlets()).thenReturn(servletEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(bootstrap.getMetricRegistry()).thenReturn(mock(MetricRegistry.class));
        when(servletEnvironment.addServlet(anyString(), any(HttpServlet.class))).thenReturn(servlet);
        when(jaxwsEnvironment.buildServlet()).thenReturn(mock(HttpServlet.class));
        when(jaxwsEnvironment.getDefaultPath()).thenReturn("/soap");
    }

    @Test
    public void constructorArgumentChecks() {
        try {
            new JAXWSBundle(null, null);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            new JAXWSBundle("soap", null);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    public void initializeAndRun() {
        JAXWSBundle jaxwsBundle = new JAXWSBundle("/soap", jaxwsEnvironment);

        try {
            jaxwsBundle.run(null);
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        jaxwsBundle.initialize(bootstrap);
        verify(jaxwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jaxwsBundle.run(environment);
        verify(servletEnvironment).addServlet(eq("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
    }

    @Test
    public void publishEndpoint() {
        JAXWSBundle jaxwsBundle = new JAXWSBundle("/soap", jaxwsEnvironment);
        Object service = new Object();
        try {
            jaxwsBundle.publishEndpoint("foo", null);
            fail();
        }

        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.publishEndpoint(null, service);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.publishEndpoint("   ", service);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        jaxwsBundle.publishEndpoint("foo", service);
        verify(jaxwsEnvironment).publishEndpoint("foo", service, null, null);

        BasicAuthentication auth = mock(BasicAuthentication.class);
        jaxwsBundle.publishEndpoint("boo", service, auth);
        verify(jaxwsEnvironment).publishEndpoint("boo", service, auth, null);

        SessionFactory sessionFactory = mock(SessionFactory.class);
        jaxwsBundle.publishEndpoint("boo", service, auth, sessionFactory);
        verify(jaxwsEnvironment).publishEndpoint("boo", service, auth, sessionFactory);

        jaxwsBundle.publishEndpoint("boo", service, sessionFactory);
        verify(jaxwsEnvironment).publishEndpoint("boo", service, null, sessionFactory);
    }

    @Test
    public void getClient() {
        JAXWSBundle jaxwsBundle = new JAXWSBundle("/soap", jaxwsEnvironment);

        Class<?> cls = Object.class;
        String url = "http://foo";


        try {
            jaxwsBundle.getClient(null, null);
            fail();
        }

        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.getClient(null, url);
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.getClient(cls, "   ");
            fail();
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        jaxwsBundle.getClient(cls, url);
        verify(jaxwsEnvironment).getClient(cls, url);

        Handler mockHandler = mock(Handler.class);
        jaxwsBundle.getClient(cls, url, mockHandler);
        verify(jaxwsEnvironment).getClient(cls, url, mockHandler);
    }

}
