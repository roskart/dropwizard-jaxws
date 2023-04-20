package com.roskart.dropwizard.jaxws.example.resources;

import com.roskart.dropwizard.jaxws.example.ws.JavaFirstService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.ws.BindingProvider;

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
