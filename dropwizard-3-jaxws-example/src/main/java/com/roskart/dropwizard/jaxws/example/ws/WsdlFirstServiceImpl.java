package com.roskart.dropwizard.jaxws.example.ws;

import com.codahale.metrics.annotation.Metered;
import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.jaxws.ServerAsyncResponse;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.Echo;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.EchoResponse;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.NonBlockingEcho;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import java.util.concurrent.Future;

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

    @Override
    @UseAsyncMethod
    @Metered
    public EchoResponse nonBlockingEcho(NonBlockingEcho parameters) {
        EchoResponse response = new EchoResponse();
        response.setValue("Blocking: " + parameters.getValue());
        return response;
    }

    public Future<EchoResponse> nonBlockingEchoAsync(
        final NonBlockingEcho parameters,
        final AsyncHandler<EchoResponse> asyncHandler) {

        final ServerAsyncResponse<EchoResponse> sar = new ServerAsyncResponse<>();

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    EchoResponse response = new EchoResponse();
                    response.setValue("Non-blocking: " + parameters.getValue());
                    sar.set(response);
                } catch (InterruptedException e) {
                    sar.exception(e);
                }
                asyncHandler.handleResponse(sar);
            }
        }.start();

        return sar;
    }
}
