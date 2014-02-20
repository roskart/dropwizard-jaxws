package com.roskart.dropwizard.jaxws.example.ws;

import com.codahale.metrics.annotation.Metered;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.Echo;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.EchoResponse;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

import javax.jws.HandlerChain;
import javax.jws.WebService;

@WebService(endpointInterface = "ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService",
        targetNamespace = "http://com.roskart.dropwizard.jaxws.example.ws/WsdlFirstService",
        name = "WsdlFirstService",
        wsdlLocation = "META-INF/WsdlFirstService.wsdl")
@HandlerChain(file="wsdlfirstservice-handlerchain.xml")
public class WsdlFirstServiceImpl implements WsdlFirstService {
    @Override
    @Metered
    public EchoResponse echo(Echo parameters) {
        EchoResponse response = new EchoResponse();
        response.setValue(parameters.getValue());
        return response;
    }

}
