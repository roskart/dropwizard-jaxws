package com.roskart.dropwizard.jaxws;

import com.yammer.dropwizard.validation.Validator;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.Handler;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Performs CXF Bus setup and provides methods for publishing JAX-WS endpoints and create JAX-WS clients.
 */
public class JAXWSEnvironment {

    private static Logger log = LoggerFactory.getLogger(JAXWSEnvironment.class);

    protected final Bus bus;
    protected final String defaultPath;
    private InstrumentedInvokerFactory instrumentedInvokerBuilder = new InstrumentedInvokerFactory();
    private UnitOfWorkInvokerFactory unitOfWorkInvokerBuilder = new UnitOfWorkInvokerFactory();

    public String getDefaultPath() {
        return this.defaultPath;
    }

    public JAXWSEnvironment(String defaultPath) {

        System.setProperty("org.apache.cxf.Logger", "org.apache.cxf.common.logging.Slf4jLogger");
        /*
        Instruct CXF to use CXFBusFactory instead of SpringBusFactory. CXFBusFactory provides ExtensionManagerBus
        which loads extension based on contents of META-INF/cxf/bus-extensions.txt file. Many CXF modules contain
        such file. When building shaded jar for dropwizard service, these files have to be merged into single
        bus-extension.txt file by using AppendingTransformer with Maven shade plugin.
        */
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");
        this.bus = BusFactory.newInstance().createBus();

        this.defaultPath = defaultPath.replace("/*", "");
    }

    public HttpServlet buildServlet() {
        CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        cxf.setBus(bus);
        return cxf;
    }

    public void setInstrumentedInvokerBuilder(InstrumentedInvokerFactory instrumentedInvokerBuilder) {
        this.instrumentedInvokerBuilder = instrumentedInvokerBuilder;
    }

    public void setUnitOfWorkInvokerBuilder(UnitOfWorkInvokerFactory unitOfWorkInvokerBuilder) {
        this.unitOfWorkInvokerBuilder = unitOfWorkInvokerBuilder;
    }

    protected BasicAuthenticationInterceptor createBasicAuthenticationInterceptor() {
        return new BasicAuthenticationInterceptor();
    }

    protected ValidatingInvoker createValidatingInvoker(Invoker invoker) {
        return new ValidatingInvoker(invoker, new Validator());
    }

    public void logEndpoints() {
        ServerRegistry sr = bus.getExtension(org.apache.cxf.endpoint.ServerRegistry.class);
        if (sr.getServers().size() > 0) {
            String endpoints = "";
            for (Server s : sr.getServers()) {
                endpoints += "    " + this.defaultPath +  s.getEndpoint().getEndpointInfo().getAddress() +
                        " (" + s.getEndpoint().getEndpointInfo().getInterface().getName() + " )" + "\n";
            }
            log.info("The following JAX-WS service endpoints were registered:\n\n" + endpoints);
        }
        else {
            log.info("No JAX-WS service endpoints were registered.");
        }
    }

    /* Publish JAX-WS endpoints */

    public void publishEndpoint(String path, Object service) {
        this.publishEndpoint(path, service, null, null);
    }

    public void publishEndpoint(String path, Object service, SessionFactory sessionFactory) {
        this.publishEndpoint(path, service, null, sessionFactory);
    }

    public void publishEndpoint(String path, Object service, BasicAuthentication authentication) {
        this.publishEndpoint(path, service, authentication, null);
    }

    public void publishEndpoint(String path, Object service,
                                BasicAuthentication authentication, SessionFactory sessionFactory) {

        checkArgument(service != null, "Service is null");
        checkArgument(path != null, "Path is null");
        checkArgument((path).trim().length() > 0, "Path is empty");
        if (!path.startsWith("local:")) { // local transport is used in tests
            path = (path.startsWith("/")) ? path : "/" + path;
        }

        Endpoint.publish(path, service);

        org.apache.cxf.endpoint.Endpoint cxfendpoint = null;

        ServerRegistry sr = bus.getExtension(org.apache.cxf.endpoint.ServerRegistry.class);

        for (Server s : sr.getServers()) {
            Class endpointclass = ((Class)s.getEndpoint().getService().get("endpoint.class"));
            if (service.getClass().getName().equals(endpointclass.getName()) ||
                 (endpointclass.isInterface() && endpointclass.isAssignableFrom(service.getClass()))) {
                cxfendpoint = s.getEndpoint();
                break;
            }
        }

        if (cxfendpoint == null) {
            throw new RuntimeException("Error publishing endpoint for service: " + service.getClass().getSimpleName() +
                    ": " + "Matching 'endpoint.class' not found.");
        }

        Invoker invoker = cxfendpoint.getService().getInvoker();

        // validating invoker
        invoker = this.createValidatingInvoker(invoker);

        if (sessionFactory != null) {
            // Add invoker to handle UnitOfWork annotations. Note that this invoker is set up before
            // instrumented invoker(s) in order for instrumented invoker(s) to wrap "unit of work" invoker.
            invoker = unitOfWorkInvokerBuilder.create(service, invoker, sessionFactory);
            cxfendpoint.getService().setInvoker(invoker);
        }

        // Replace CXF service invoker with instrumented invoker(s)
        invoker = instrumentedInvokerBuilder.create(service, invoker);
        cxfendpoint.getService().setInvoker(invoker);

        if (authentication != null) {
            // Configure CXF in interceptor to handle basic authentication
            BasicAuthenticationInterceptor basicAuthInterceptor = this.createBasicAuthenticationInterceptor();
            basicAuthInterceptor.setAuthenticator(authentication);
            cxfendpoint.getInInterceptors().add(basicAuthInterceptor);
        }
    }

    /* JAX-WS client factory */

    public <T> T getClient(Class<T> serviceClass, String address, Handler ... handlers) {

        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setServiceClass(serviceClass);
        proxyFactory.setAddress(address);

        for (Handler h : handlers) {
            proxyFactory.getHandlers().add(h);
        }

        T proxy = (T)proxyFactory.create();

        HTTPConduit http = (HTTPConduit)ClientProxy.getClient(proxy).getConduit();
        HTTPClientPolicy client = http.getClient();

        //TODO: configurable connection timeout
        client.setConnectionTimeout(500);
        //TODO: configurable receive timeout
        client.setReceiveTimeout(2000);

        return (T)proxy;
    }

}
