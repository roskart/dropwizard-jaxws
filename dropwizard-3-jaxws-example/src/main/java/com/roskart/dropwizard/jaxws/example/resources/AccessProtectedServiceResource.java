package com.roskart.dropwizard.jaxws.example.resources;

import com.roskart.dropwizard.jaxws.example.ws.JavaFirstService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.BindingProvider;

/**
 * Dropwizard resource that invokes JavaFirstService SOAP web service using basic authentication.
 */
@Path("/javafirstclient")
@Produces(MediaType.APPLICATION_JSON)

public class AccessProtectedServiceResource {

    JavaFirstService javaFirstService;

    public AccessProtectedServiceResource(JavaFirstService javaFirstService) {
        this.javaFirstService = javaFirstService;
    }

    @GET
    public String getEcho() {
        try {

            BindingProvider bp = (BindingProvider)javaFirstService;
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "johndoe");
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "secret");

            return this.javaFirstService.echo("Hello from the protected service!");
        }
        catch(JavaFirstService.JavaFirstServiceException jfse) {
            throw new WebApplicationException(jfse);
        }
    }

}
