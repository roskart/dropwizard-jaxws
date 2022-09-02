package com.roskart.dropwizard.jaxws;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.Configuration;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class JAXWSBundleTest {

    Environment environment = mock(Environment.class);
    Bootstrap<?> bootstrap = mock(Bootstrap.class);
    ServletEnvironment servletEnvironment = mock(ServletEnvironment.class);
    ServletRegistration.Dynamic servlet = mock(ServletRegistration.Dynamic.class);
    JAXWSEnvironment jaxwsEnvironment = mock(JAXWSEnvironment.class);
    LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);

    @BeforeEach
    public void setUp() {
        when(environment.servlets()).thenReturn(servletEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(bootstrap.getMetricRegistry()).thenReturn(mock(MetricRegistry.class));
        when(servletEnvironment.addServlet(anyString(), any(HttpServlet.class))).thenReturn(servlet);
        when(jaxwsEnvironment.buildServlet()).thenReturn(mock(HttpServlet.class));
        when(jaxwsEnvironment.getDefaultPath()).thenReturn("/soap");
    }

    @Test
    void constructorArgumentChecks() {
        try {
            new JAXWSBundle<>(null, null);
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            new JAXWSBundle<>("soap", null);
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }
    }

    @Test
    void initializeAndRun() {
        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);

        try {
            jaxwsBundle.run(null, null);
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        jaxwsBundle.initialize(bootstrap);
        verify(jaxwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jaxwsBundle.run(null, environment);
        verify(servletEnvironment).addServlet(startsWith("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
        verify(jaxwsEnvironment, never()).setPublishedEndpointUrlPrefix(anyString());
    }

    @Test
    void initializeAndRunWithPublishedEndpointUrlPrefix() {
        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<Configuration>("/soap", jaxwsEnvironment) {
            @Override
            protected String getPublishedEndpointUrlPrefix(Configuration configuration) {
                return "http://some/prefix";
            }
        };

        try {
            jaxwsBundle.run(null, null);
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        jaxwsBundle.initialize(bootstrap);
        verify(jaxwsEnvironment).setInstrumentedInvokerBuilder(any(InstrumentedInvokerFactory.class));

        jaxwsBundle.run(null, environment);
        verify(servletEnvironment).addServlet(startsWith("CXF Servlet"), any(Servlet.class));
        verify(lifecycleEnvironment).addServerLifecycleListener(any(ServerLifecycleListener.class));
        verify(servlet).addMapping("/soap/*");
        verify(jaxwsEnvironment).setPublishedEndpointUrlPrefix("http://some/prefix");
    }

    @Test
    void publishEndpoint() {

        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);
        Object service = new Object();
        try {
            jaxwsBundle.publishEndpoint(new EndpointBuilder("foo", null));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.publishEndpoint(new EndpointBuilder(null, service));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.publishEndpoint(new EndpointBuilder("   ", service));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        EndpointBuilder builder = mock(EndpointBuilder.class);
        jaxwsBundle.publishEndpoint(builder);
        verify(jaxwsEnvironment).publishEndpoint(builder);
    }

    @Test
    void getClient() {

        JAXWSBundle<?> jaxwsBundle = new JAXWSBundle<>("/soap", jaxwsEnvironment);

        Class<?> cls = Object.class;
        String url = "http://foo";

        try {
            jaxwsBundle.getClient(new ClientBuilder<>(null, null));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.getClient(new ClientBuilder<>(null, url));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        try {
            jaxwsBundle.getClient(new ClientBuilder<>(cls, "   "));
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        ClientBuilder<?> builder = new ClientBuilder<>(cls, url);
        jaxwsBundle.getClient(builder);
        verify(jaxwsEnvironment).getClient(builder);
    }
}
