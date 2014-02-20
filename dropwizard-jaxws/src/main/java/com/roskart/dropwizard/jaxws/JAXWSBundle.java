package com.roskart.dropwizard.jaxws;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import org.eclipse.jetty.server.Server;
import org.hibernate.SessionFactory;

import javax.xml.ws.handler.Handler;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A Dropwizard bundle that enables Dropwizard applications to publish SOAP web services using JAX-WS and create
 * web service clients.
 */
public class JAXWSBundle implements Bundle {

    protected static final String DEFAULT_PATH = "/soap";
    protected JAXWSEnvironment jaxwsEnvironment;
    protected String servletPath;

    /**
     * Initialize JAXWSEnvironment. Service endpoints are published relative to '/soap'.
     */
    public JAXWSBundle() {
        this(DEFAULT_PATH);
    }

    /**
     * Initialize JAXWSEnvironment. Service endpoints are published relative to the provided servletPath.
     * @param servletPath Root path for service endpoints. Leading slash is required.
     */
    public JAXWSBundle(String servletPath) {
        this(servletPath, new JAXWSEnvironment(servletPath));
    }

    /**
     * Use provided JAXWSEnvironment. Service endpoints are published relative to the provided servletPath.
     * @param servletPath Root path for service endpoints. Leading slash is required.
     * @param jaxwsEnvironment Valid JAXWSEnvironment.
     */
    public JAXWSBundle(String servletPath, JAXWSEnvironment jaxwsEnvironment) {
        checkArgument(servletPath != null, "Servlet path is null");
        checkArgument(servletPath.startsWith("/"), "%s is not an absolute path", servletPath);
        checkArgument(jaxwsEnvironment != null, "jaxwsEnvironment is null");
        this.servletPath = servletPath.endsWith("/") ? servletPath + "*" : servletPath + "/*";
        this.jaxwsEnvironment = jaxwsEnvironment;
    }

    /**
     * Implements com.yammer.dropwizard.Bundle#initialize()
     */
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        this.jaxwsEnvironment.setInstrumentedInvokerBuilder(
                new InstrumentedInvokerFactory(bootstrap.getMetricRegistry()));
    }

    /**
     * Implements com.yammer.dropwizard.Bundle#run()
     */
    @Override
    public void run(Environment environment) {
        checkArgument(environment != null, "Environment is null");
        environment.servlets().addServlet("CXF Servlet", jaxwsEnvironment.buildServlet()).addMapping(servletPath);
        environment.lifecycle().addServerLifecycleListener(new ServerLifecycleListener() {
            @Override
            public void serverStarted(Server server) {
                jaxwsEnvironment.logEndpoints();
            }
        });
    }

    /**
     * Publish JAX-WS endpoint. Endpoint is published relative to the CXF servlet path.
     * @param path Relative endpoint path.
     * @param service Service implementation.
     */
    public void publishEndpoint(String path, Object service) {
        this.publishEndpoint(path, service, null, null);
    }

    /**
     * Publish JAX-WS endpoint with Dropwizard Hibernate Bundle integration. Service is scanned for @UnitOfWork
     * annotations. Endpoint is published relative to the CXF servlet path.
     * @param path Relative endpoint path.
     * @param service Service implementation.
     * @param sessionFactory Hibernate session factory.
     */
    public void publishEndpoint(String path, Object service, SessionFactory sessionFactory) {
        this.publishEndpoint(path, service, null, sessionFactory);
    }

    /**
     * Publish JAX-WS protected endpoint using Dropwizard BasicAuthentication. Endpoint is published relative
     * to the CXF servlet path.
     * @param path Relative endpoint path.
     * @param service Service implementation.
     * @param authentication BasicAuthentication implementation.
     */
    public void publishEndpoint(String path, Object service, BasicAuthentication authentication) {
        this.publishEndpoint(path, service, authentication, null);
    }

    /**
     * Publish JAX-WS protected endpoint using Dropwizard BasicAuthentication with Dropwizard Hibernate Bundle
     * integration. Service is scanned for @UnitOfWork annotations. Endpoint is published relative to the CXF
     * servlet path.
     * @param path Relative endpoint path.
     * @param service Service implementation.
     * @param authentication BasicAuthentication implementation.
     * @param sessionFactory Hibernate session factory.
     */
    public void publishEndpoint(String path, Object service, BasicAuthentication authentication,
                                SessionFactory sessionFactory) {
        checkArgument(service != null, "Service is null");
        checkArgument(path != null, "Path is null");
        checkArgument((path).trim().length() > 0, "Path is empty");
        jaxwsEnvironment.publishEndpoint(
                path,
                service,
                authentication,
                sessionFactory);
    }

    /**
     * Factory method for creating JAX-WS clients.
     * @param serviceClass Service interface class.
     * @param address Endpoint URL address.
     * @param handlers Client side JAX-WS handlers. Optional.
     * @param <T> Service interface type.
     * @return JAX-WS client proxy.
     */
    public <T> T getClient(Class<T> serviceClass, String address, Handler...handlers) {
        checkArgument(serviceClass != null, "ServiceClass is null");
        checkArgument(address != null, "Address is null");
        checkArgument((address).trim().length() > 0, "Address is empty");
        return jaxwsEnvironment.<T>getClient(serviceClass, address, handlers);
    }

}
