package com.roskart.dropwizard.jaxws.example.ws;

import com.codahale.metrics.annotation.Metered;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class SimpleService {

    @WebMethod
    @Metered
    public String echo(String input) {
        return input;
    }
}
