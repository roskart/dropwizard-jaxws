package com.roskart.dropwizard.jaxws.example;

import com.roskart.dropwizard.jaxws.BasicAuthentication;
import com.roskart.dropwizard.jaxws.JAXWSBundle;
import com.roskart.dropwizard.jaxws.example.auth.BasicAuthenticator;
import com.roskart.dropwizard.jaxws.example.core.Person;
import com.roskart.dropwizard.jaxws.example.db.PersonDAO;
import com.roskart.dropwizard.jaxws.example.resources.*;
import com.roskart.dropwizard.jaxws.example.ws.*;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import com.yammer.dropwizard.hibernate.HibernateBundle;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

public class JaxWsExampleService extends Service<JaxWsExampleServiceConfiguration> {

    // HibernateBundle is used by HibernateExampleService
    private final HibernateBundle<JaxWsExampleServiceConfiguration> hibernate = new HibernateBundle<JaxWsExampleServiceConfiguration>(Person.class) {
        @Override
        public DatabaseConfiguration getDatabaseConfiguration(JaxWsExampleServiceConfiguration configuration) {
            return configuration.getDatabaseConfiguration();
        }
    };

    // JAX-WS Bundle
    private JAXWSBundle jaxWsBundle = new JAXWSBundle();

    public static void main(String[] args) throws Exception {
        new JaxWsExampleService().run(args);
    }

    @Override
    public void initialize(Bootstrap<JaxWsExampleServiceConfiguration> bootstrap) {
        bootstrap.setName("dropwizard-jaxws-example");
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(jaxWsBundle);
    }

    @Override
    public void run(JaxWsExampleServiceConfiguration jaxWsExampleServiceConfiguration, Environment environment) throws Exception {

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
        environment.addResource(new AccessWsdlFirstServiceResource(
                jaxWsBundle.getClient(
                        WsdlFirstService.class,
                        "http://localhost:8080/soap/wsdlfirst",
                        new WsdlFirstClientHandler())));

        // RESTful resource that invokes JavaFirstService on localhost and uses basic authentication.
        environment.addResource(new AccessProtectedServiceResource(
                jaxWsBundle.getClient(
                        JavaFirstService.class,
                        "http://localhost:8080/soap/javafirst"
                )
        ));
    }
}
