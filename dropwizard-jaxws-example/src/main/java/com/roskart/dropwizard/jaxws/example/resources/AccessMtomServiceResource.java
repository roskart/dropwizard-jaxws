package com.roskart.dropwizard.jaxws.example.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.cxf.helpers.IOUtils;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.Hello;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.HelloResponse;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.MtomService;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.ObjectFactory;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;

import jakarta.mail.util.ByteArrayDataSource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/mtomclient")
@Produces(MediaType.APPLICATION_JSON)
public class AccessMtomServiceResource {

    MtomService mtomServiceClient;

    public AccessMtomServiceResource(MtomService mtomServiceClient) {
        this.mtomServiceClient = mtomServiceClient;
    }

    @GET
    @Timed
    public String getFoo() {

        ObjectFactory of = new ObjectFactory();
        Hello h = of.createHello();
        h.setTitle("Hello");
        h.setBinary(new DataHandler((DataSource) new ByteArrayDataSource("test".getBytes(), "text/plain")));

        HelloResponse hr = mtomServiceClient.hello(h);

        try {
            return "Hello response: " + hr.getTitle() + ", " +
                    IOUtils.readStringFromStream(hr.getBinary().getInputStream());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
