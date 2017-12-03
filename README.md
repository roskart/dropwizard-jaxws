Dropwizard-JAXWS
================

Dropwizard-JAXWS is a [Dropwizard](https://github.com/codahale/dropwizard) Bundle that enables building SOAP web
services and clients using JAX-WS API with Dropwizard.

Features
--------
* Uses [Apache CXF](http://cxf.apache.org/) web services framework (no Spring Framework dependency).
* Java First and WSDL first service development.
* Using standard JAX-WS annotations, without custom deployment descriptors.
* [Metrics](https://github.com/codahale/metrics) instrumentation: @Metered, @Timed and @ExceptionMetered annotations.
* Dropwizard validation support.
* Dropwizard Hibernate support (@UnitOfWork).
* Dropwizard basic authentication using Dropwizard Authenticator.
* Web service client factory.
* Support for JAX-WS handlers, MTOM, CXF interceptors(both client and server side) and CXF @UseAsyncMethod annotation.

Using
-----

To use dropwizard-jaxws in your project, add the following dependency to your `pom.xml`:

        <dependency>
            <groupId>com.github.roskart.dropwizard-jaxws</groupId>
            <artifactId>dropwizard-jaxws</artifactId>
            <version>1.0.4</version>
        </dependency>

Hello World
-----------

**SOAP service:**

        @Metered
        @WebService
        public HelloWorldSOAP {
            @WebMethod
            public String sayHello() {
                return "Hello world!";
            }
        }

**Dropwizard application:**

        public class MyApplication extends Application<MyApplicationConfiguration> {

            private JAXWSBundle jaxWsBundle = new JAXWSBundle();

            @Override
            public void initialize(Bootstrap<MyApplicationConfiguration> bootstrap) {
                bootstrap.addBundle(jaxWsBundle);
            }

            @Override
            public void run(MyApplicationConfiguration configuration, Environment environment) throws Exception {
                jaxWsBundle.publishEndpoint(
                    new EndpointBuilder("/hello", new HelloWorldSOAP()));
            }

            public static void main(String[] args) throws Exception {
                new MyApplication().run(args);
            }
        }

Client
------

Using HelloWorldSOAP web service client:

        HelloWorldSOAP helloWorld = jaxWsBundle.getClient(
            new ClientBuilder(HelloWorldSOAP.class, "http://server/path"));
        System.out.println(helloWorld.sayHello());

Examples
--------
Module `dropwizard-jaxws-example` contains Dropwizard application (`JaxWsExampleApplication`) with the following SOAP
web services and RESTful resources:

* **SimpleService**: A minimal 'hello world' example.

* **JavaFirstService**: Java first development example. `JavaFirstService` interface uses JAX-WS annotations.
`JavaFirstServiceImpl` contains service implementation instrumented with Metrics annotations. Service is secured with
basic authentication using `dropwizard-auth`. `BasicAuthenticator` implements Dropwizard `Authenticator`.
`JavaFirstServiceImpl` accesses authenticated user properties via injected JAX-WS `WebServiceContext`.

* **WsdlFirstService**: WSDL first development example. WSDL is stored in `resources/META-INF/WsdlFirstService.wsdl`.
Code is generated using `cxf-codegen-plugin` which is configured in `pom.xml`. `WsdlFirstServiceImpl` contains service
implementation with blocking and non-blocking methods. `WsdlFirstServiceHandler` contains server-side JAX-WS handler.

* **HibernateExampleService**: `dropwizard-hibernate` example. `HibernateExampleService` implements the service.
`@UnitOfWork` annotations are used for defining transactional boundaries. `@Valid` annotation is used for parameter
validation on `createPerson` method. `HibernateExampleService` accesses the database through `PersonDAO`. Embedded H2
database is used. Database configuration is stored in Dropwizard config file `config.yaml`.

* **MtomService**: WSDL first MTOM attachment example. WSDL is stored in `resources/META-INF/MtomService.wsdl`.
Code is generated using `cxf-codegen-plugin` which is configured in `pom.xml`. `MtomServiceImpl` contains service
implementation with MTOM enabled.

* **AccessProtectedServiceResource**: Dropwizard RESTful service which uses `JavaFirstService` client to invoke
`JavaFirstService` SOAP web service on the same host. User credentials are provided to access protected service.

* **AccessWsdlFirstServiceResource**: Dropwizard RESTful service which uses `WsdlFirstService` client to invoke
`WsdlFirstService` SOAP web service on the same host. `WsdlFirstClientHandler` contains client-side JAX-WS handler.

* **AccessMtomServiceResource**: Dropwizard RESTful service which uses `MtomService` client to invoke
`MtomService` SOAP web service on the same host as an example for client side MTOM support.

* See `JaxWsExampleApplication` for examples on usage of client side JAX-WS handler and CXF interceptors.

### Running the examples:

After cloning the repository, go to the dropwizard-jaxws root folder and run:

        mvn package

To run the example service:

        java -jar dropwizard-jaxws-example\target\dropwizard-jaxws-example-1.0.4.jar server dropwizard-jaxws-example\config.yaml

Notes
-----

### Building FAT jar

When using `maven-shade-plugin` for building fat jar, you must add the following `transformer` element to plugin
configuration:

        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
            <resource>META-INF/cxf/bus-extensions.txt</resource>
        </transformer>

For example on building fat jar, see `dropwizard-jaxws-example/pom.xml`.

When using Gradle and a recent version of [shadowJar](https://github.com/johnrengelman/shadow) use the following snippet:

    shadowJar {
        // ...
        append('META-INF/cxf/bus-extensions.txt')
    }
    
License
-------
Apache Software License 2.0, see [LICENSE](https://github.com/roskart/dropwizard-jaxws/blob/master/LICENSE).

Changelog
---------

### v1.0.4

- JAXWSBundle now returns JAX-WS endpoint (see Issue #13).

### v1.0.3

- Support for providing a property bag to JAX-WS endpoint (see Issue #13).
- Upgraded to Dropwizard 1.2.1.
- Upgraded to CXF 3.2.1.

### v1.0.2

- Upgraded to Dropwizard 1.1.0.
- Upgraded to CXF 3.1.11.

### v1.0.1

- Upgraded to Dropwizard 1.0.2.

### v1.0.0

- Upgraded to Dropwizard 1.0.0.
- Upgraded to CXF 3.1.6.
- Java 8 is used by default.
- Added support for publishedEndpointUrl (see Pull request #9).

### v0.10.2

- Added support for CXF @UseAsyncMethod annotation (see Pull request #8).

### v0.10.1

- Added support for multiple JAXWSBundle instances (see Issue #7).

### v0.10.0

- Upgraded to Dropwizard 0.9.2.

### v0.9.0

- Upgraded to Dropwizard 0.9.1.
- Upgraded to CXF 3.1.4.

### v0.8.0

- Project is now released to Maven Central. Maven coordinates were changed.

### v0.7.0

- Upgraded to Dropwizard 0.8.1.

### v0.6.0

- Upgraded to Dropwizard 0.8.0.
- Upgraded to CXF 3.0.4.

### v0.5.0

- Upgraded to Dropwizard 0.7.1.
- Upgraded to CXF 3.0.0.

### v0.4.0

- Added MTOM support and examples.

### v0.3.0

- Updated JAXWSBundle API: introduced EndpointBuilder and ClientBuilder.
- Added suport for CXF interceptors.

### v0.2.0

- Upgraded to Dropwizard 0.7.0.
- Upgraded to CXF 2.7.8.

### v0.1.0

- Initial Release (uses Dropwizard 0.6.2).
