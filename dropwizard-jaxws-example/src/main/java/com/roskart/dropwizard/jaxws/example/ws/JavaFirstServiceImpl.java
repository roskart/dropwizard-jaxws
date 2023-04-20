package com.roskart.dropwizard.jaxws.example.ws;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import java.security.Principal;

@WebService(name="JavaFirstService",
        serviceName="JavaFirstService",
        portName="JavaFirstService",
        targetNamespace="http://com.roskart.dropwizard.example/JavaFirstService",
        endpointInterface= "com.roskart.dropwizard.jaxws.example.ws.JavaFirstService")
public class JavaFirstServiceImpl implements JavaFirstService {

    @Resource
    WebServiceContext wsContext;

    @Override
    @Metered
    @ExceptionMetered
    public String echo(String in) throws JavaFirstServiceException {
        if (in == null || in.trim().length() == 0) {
            throw new JavaFirstServiceException("Invalid parameter");
        }

        Principal user = (Principal)wsContext.getMessageContext().get("dropwizard.jaxws.principal");
        return in + "; principal: " + user.getName();
    }
}
