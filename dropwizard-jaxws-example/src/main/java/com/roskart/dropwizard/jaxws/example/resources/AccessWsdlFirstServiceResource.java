package com.roskart.dropwizard.jaxws.example.resources;

import com.codahale.metrics.annotation.Timed;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.Echo;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.EchoResponse;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.ObjectFactory;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Dropwizard resource that invokes WsdlFirstService SOAP web service.
 * @see WsdlFirstClientHandler
 */
@Path("/wsdlfirstclient")
@Produces(MediaType.APPLICATION_JSON)
public class AccessWsdlFirstServiceResource {

    WsdlFirstService wsdlFirstServiceClient;

    public AccessWsdlFirstServiceResource(WsdlFirstService wsdlFirstServiceClient) {
        this.wsdlFirstServiceClient = wsdlFirstServiceClient;
    }

    @GET
    @Timed
    public String getFoo() {

        ObjectFactory of = new ObjectFactory();
        Echo e = of.createEcho();
        e.setValue("echo value");

        EchoResponse er = wsdlFirstServiceClient.echo(e);

        return "Echo response: " + er.getValue();
    }
}
