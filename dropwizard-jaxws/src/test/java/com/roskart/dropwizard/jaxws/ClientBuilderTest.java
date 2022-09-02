package com.roskart.dropwizard.jaxws;

import org.apache.cxf.interceptor.Interceptor;
import org.junit.jupiter.api.Test;

import javax.xml.ws.handler.Handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;

public class ClientBuilderTest {

    @Test
    void buildClient() {

        Handler<?> handler = mock(Handler.class);

        Interceptor<?> inInterceptor = mock(Interceptor.class);
        Interceptor<?> inFaultInterceptor = mock(Interceptor.class);
        Interceptor<?> outInterceptor = mock(Interceptor.class);
        Interceptor<?> outFaultInterceptor = mock(Interceptor.class);

        ClientBuilder<Object> builder = new ClientBuilder<>(Object.class, "address")
                .connectTimeout(1234)
                .receiveTimeout(5678)
                .handlers(handler, handler)
                .bindingId("binding id")
                .cxfInInterceptors(inInterceptor, inInterceptor)
                .cxfInFaultInterceptors(inFaultInterceptor, inFaultInterceptor)
                .cxfOutInterceptors(outInterceptor, outInterceptor)
                .cxfOutFaultInterceptors(outFaultInterceptor, outFaultInterceptor);

        assertThat(builder.getAddress(), equalTo("address"));
        assertThat(builder.getServiceClass(), equalTo(Object.class));
        assertThat(builder.getConnectTimeout(), equalTo(1234));
        assertThat(builder.getReceiveTimeout(), equalTo(5678));
        assertThat(builder.getBindingId(), equalTo("binding id"));
        assertThat(builder.getCxfInInterceptors(), contains(inInterceptor, inInterceptor));
        assertThat(builder.getCxfInFaultInterceptors(), contains(inFaultInterceptor, inFaultInterceptor));
        assertThat(builder.getCxfOutInterceptors(), contains(outInterceptor, outInterceptor));
        assertThat(builder.getCxfOutFaultInterceptors(), contains(outFaultInterceptor, outFaultInterceptor));
    }
}
