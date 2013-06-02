package com.roskart.dropwizard.jaxws;

import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServlet;
import javax.xml.ws.handler.Handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JAXWSBundleTest {

    Environment environment = mock(Environment.class);
    JAXWSEnvironment jaxwsEnvironment = mock(JAXWSEnvironment.class);

    @Before
    public void setUp() {
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
    public void run() {
        JAXWSBundle jaxwsBundle = new JAXWSBundle("/soap", jaxwsEnvironment);

        try {
            jaxwsBundle.run(null);
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }

        jaxwsBundle.run(environment);
        verify(environment).addServlet(any(HttpServlet.class), eq("/soap/*"));
        verify(environment).addServerLifecycleListener(any(ServerLifecycleListener.class));
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
