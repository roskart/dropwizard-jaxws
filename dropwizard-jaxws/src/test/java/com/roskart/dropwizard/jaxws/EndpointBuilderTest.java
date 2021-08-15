package com.roskart.dropwizard.jaxws;

import org.apache.cxf.interceptor.Interceptor;
import org.hibernate.SessionFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
public class EndpointBuilderTest {

    @Test
    public void buildEndpoint() {
        Object service = new Object();
        String path = "/foo";
        String publishedUrl = "http://external/url";
        BasicAuthentication basicAuth = mock(BasicAuthentication.class);
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Interceptor<?> inInterceptor = mock(Interceptor.class);
        Interceptor<?> inFaultInterceptor = mock(Interceptor.class);
        Interceptor<?> outInterceptor = mock(Interceptor.class);
        Interceptor<?> outFaultInterceptor = mock(Interceptor.class);
        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");

        EndpointBuilder builder = new EndpointBuilder(path, service)
                .publishedEndpointUrl(publishedUrl)
                .authentication(basicAuth)
                .sessionFactory(sessionFactory)
                .cxfInInterceptors(inInterceptor, inInterceptor)
                .cxfInFaultInterceptors(inFaultInterceptor, inFaultInterceptor)
                .cxfOutInterceptors(outInterceptor, outInterceptor)
                .cxfOutFaultInterceptors(outFaultInterceptor, outFaultInterceptor)
                .properties(props);

        assertThat(builder.getPath(), equalTo(path));
        assertThat(builder.getService(), equalTo(service));
        assertThat(builder.publishedEndpointUrl(), equalTo(publishedUrl));
        assertThat(builder.getAuthentication(), equalTo(basicAuth));
        assertThat(builder.getSessionFactory(), equalTo(sessionFactory));
        assertThat(builder.getCxfInInterceptors(), contains(new Interceptor<?>[]{ inInterceptor, inInterceptor }));
        assertThat(builder.getCxfInFaultInterceptors(), contains(new Interceptor<?>[]{ inFaultInterceptor, inFaultInterceptor }));
        assertThat(builder.getCxfOutInterceptors(), contains(new Interceptor<?>[]{ outInterceptor, outInterceptor }));
        assertThat(builder.getCxfOutFaultInterceptors(), contains(new Interceptor<?>[]{ outFaultInterceptor, outFaultInterceptor }));
        assertThat(builder.getProperties().get("key"), equalTo("value"));
    }
}
