package com.roskart.dropwizard.jaxws.example.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.WebFault;

@WebService(targetNamespace = "http://com.roskart.dropwizard.example/JavaFirstService")
public interface JavaFirstService {

    @WebFault(name = "JavaFirstServiceException")
    class JavaFirstServiceException extends Exception {
        public JavaFirstServiceException(String s) {
            super(s);
        }
    }

    @WebMethod(operationName = "Echo")
    @WebResult(name = "EchoResponse")
    String echo(@WebParam(name = "EchoParameter") String in) throws JavaFirstServiceException;
}
