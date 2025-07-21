package com.roskart.dropwizard.jaxws;

import com.google.common.collect.ImmutableList;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

/**
 * Contains common code for ClientBuilder and EndpointBuilder.
 */
public abstract class AbstractBuilder {

    protected boolean mtomEnabled = false;
    protected ImmutableList<Interceptor<? extends Message>> cxfInInterceptors;
    protected ImmutableList<Interceptor<? extends Message>> cxfInFaultInterceptors;
    protected ImmutableList<Interceptor<? extends Message>> cxfOutInterceptors;
    protected ImmutableList<Interceptor<? extends Message>> cxfOutFaultInterceptors;

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    public ImmutableList<Interceptor<? extends Message>> getCxfInInterceptors() {
        return cxfInInterceptors;
    }

    public ImmutableList<Interceptor<? extends Message>> getCxfInFaultInterceptors() {
        return cxfInFaultInterceptors;
    }

    public ImmutableList<Interceptor<? extends Message>> getCxfOutInterceptors() {
        return cxfOutInterceptors;
    }

    public ImmutableList<Interceptor<? extends Message>> getCxfOutFaultInterceptors() {
        return cxfOutFaultInterceptors;
    }

    /**
     * Add CXF interceptors to the incoming interceptor chain.
     * @param interceptors CXF interceptors.
     * @return EndpointBuilder instance.
     */
    @SuppressWarnings("unchecked")
    public AbstractBuilder cxfInInterceptors(Interceptor<? extends Message> ... interceptors) {
        this.cxfInInterceptors =
                ImmutableList.<Interceptor<? extends Message>>builder()
                        .add(interceptors).build();
        return this;
    }

    /**
     * Add CXF interceptors to the incoming fault interceptor chain.
     * @param interceptors CXF interceptors.
     * @return EndpointBuilder instance.
     */
    @SuppressWarnings("unchecked")
    public AbstractBuilder cxfInFaultInterceptors(Interceptor<? extends Message> ... interceptors) {
        this.cxfInFaultInterceptors =
                ImmutableList.<Interceptor<? extends Message>>builder()
                        .add(interceptors).build();
        return this;
    }

    /**
     * Add CXF interceptors to the outgoing interceptor chain.
     * @param interceptors CXF interceptors.
     * @return EndpointBuilder instance.
     */
    @SuppressWarnings("unchecked")
    public AbstractBuilder cxfOutInterceptors(Interceptor<? extends Message> ... interceptors) {
        this.cxfOutInterceptors =
                ImmutableList.<Interceptor<? extends Message>>builder()
                        .add(interceptors).build();
        return this;
    }

    /**
     * Add CXF interceptors to the outgoing fault interceptor chain.
     * @param interceptors CXF interceptors.
     * @return EndpointBuilder instance.
     */
    @SuppressWarnings("unchecked")
    public AbstractBuilder cxfOutFaultInterceptors(Interceptor<? extends Message> ... interceptors) {
        this.cxfOutFaultInterceptors =
                ImmutableList.<Interceptor<? extends Message>>builder()
                        .add(interceptors).build();
        return this;
    }

    /**
     * Enable MTOM for binary attachments.
     */
    public AbstractBuilder enableMtom() {
        this.mtomEnabled = true;
        return this;
    }
}
