package com.roskart.dropwizard.jaxws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class DummyService {

    @WebMethod
    public void foo() {
    }
}
