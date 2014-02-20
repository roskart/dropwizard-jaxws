package com.roskart.dropwizard.jaxws.example;

import com.roskart.dropwizard.jaxws.BasicAuthentication;
import com.roskart.dropwizard.jaxws.JAXWSBundle;
import com.roskart.dropwizard.jaxws.example.auth.BasicAuthenticator;
import com.roskart.dropwizard.jaxws.example.core.Person;
import com.roskart.dropwizard.jaxws.example.db.PersonDAO;
import com.roskart.dropwizard.jaxws.example.resources.*;
import com.roskart.dropwizard.jaxws.example.ws.*;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

public class JaxWsExampleApplication extends Application<JaxWsExampleApplicationConfiguration> {

    // HibernateBundle is used by HibernateExampleService
    private final HibernateBundle<JaxWsExampleApplicationConfiguration> hibernate = new HibernateBundle<JaxWsExampleApplicationConfiguration>(Person.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(JaxWsExampleApplicationConfiguration configuration) {
            return configuration.getDatabaseConfiguration();
        }
    };

    // JAX-WS Bundle
    private JAXWSBundle jaxWsBundle = new JAXWSBundle();

    public static void main(String[] args) throws Exception {
        new JaxWsExampleApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<JaxWsExampleApplicationConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(jaxWsBundle);
    }

    @Override
    public void run(JaxWsExampleApplicationConfiguration jaxWsExampleApplicationConfiguration, Environment environment) throws Exception {

        // Hello world service
        jaxWsBundle.publishEndpoint("/simple", new SimpleService());

        // Java first service protected with basic authentication
        jaxWsBundle.publishEndpoint("/javafirst", new JavaFirstServiceImpl(),
                new BasicAuthentication(new BasicAuthenticator(), "TOP_SECRET"));

        // WSDL first service using server side JAX-WS handler
        jaxWsBundle.publishEndpoint("/wsdlfirst", new WsdlFirstServiceImpl());

        // Service using Hibernate
        jaxWsBundle.publishEndpoint("/hibernate",
                new HibernateExampleService(new PersonDAO(hibernate.getSessionFactory())),
                hibernate.getSessionFactory());

        // RESTful resource that invokes WsdlFirstService on localhost and uses client side JAX-WS handler.
        environment.jersey().register(new AccessWsdlFirstServiceResource(
                jaxWsBundle.getClient(
                        WsdlFirstService.class,
                        "http://localhost:8080/soap/wsdlfirst",
                        new WsdlFirstClientHandler())));

        // RESTful resource that invokes JavaFirstService on localhost and uses basic authentication.
        environment.jersey().register(new AccessProtectedServiceResource(
                jaxWsBundle.getClient(
                        JavaFirstService.class,
                        "http://localhost:8080/soap/javafirst"
                )
        ));
    }
}
