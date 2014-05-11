package com.roskart.dropwizard.jaxws;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.hibernate.SessionFactory;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
public class EndpointBuilderTest {

    @Test
    public void buildEndpoint() {
        Object service = new Object();
        String path = "/foo";
        BasicAuthentication basicAuth = mock(BasicAuthentication.class);
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Interceptor<? extends Message> inInterceptor = mock(Interceptor.class);
        Interceptor<? extends Message> inFaultInterceptor = mock(Interceptor.class);
        Interceptor<? extends Message> outInterceptor = mock(Interceptor.class);
        Interceptor<? extends Message> outFaultInterceptor = mock(Interceptor.class);

        EndpointBuilder builder = new EndpointBuilder(path, service)
                .authentication(basicAuth)
                .sessionFactory(sessionFactory)
                .cxfInInterceptors(inInterceptor, inInterceptor)
                .cxfInFaultInterceptors(inFaultInterceptor, inFaultInterceptor)
                .cxfOutInterceptors(outInterceptor, outInterceptor)
                .cxfOutFaultInterceptors(outFaultInterceptor, outFaultInterceptor);

        assertThat(builder.getPath(), equalTo(path));
        assertThat(builder.getService(), equalTo(service));
        assertThat(builder.getAuthentication(), equalTo(basicAuth));
        assertThat(builder.getSessionFactory(), equalTo(sessionFactory));
        assertThat(builder.getCxfInInterceptors(), contains(inInterceptor, inInterceptor));
        assertThat(builder.getCxfInFaultInterceptors(), contains(inFaultInterceptor, inFaultInterceptor));
        assertThat(builder.getCxfOutInterceptors(), contains(outInterceptor, outInterceptor));
        assertThat(builder.getCxfOutFaultInterceptors(), contains(outFaultInterceptor, outFaultInterceptor));
    }
}
