package com.roskart.dropwizard.jaxws.example.ws;

import com.codahale.metrics.annotation.Metered;
import org.apache.cxf.helpers.IOUtils;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.Hello;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.HelloResponse;
import ws.example.jaxws.dropwizard.roskart.com.mtomservice.MtomService;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;

import jakarta.jws.WebService;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.soap.MTOM;
import java.io.IOException;

@MTOM // @MTOM annotation is not necessary if you invoke enableMtom on EndopointBuilder
@WebService(endpointInterface = "ws.example.jaxws.dropwizard.roskart.com.mtomservice.MtomService",
        targetNamespace = "http://com.roskart.dropwizard.jaxws.example.ws/MtomService",
        name = "MtomService",
        wsdlLocation = "META-INF/MtomService.wsdl")
public class MtomServiceImpl implements MtomService {
    @Metered
    @Override
    public HelloResponse hello(Hello parameters) {
        try {
            byte[] bin = IOUtils.readBytesFromStream(parameters.getBinary().getInputStream());
            HelloResponse response = new HelloResponse();
            response.setTitle(parameters.getTitle());
            response.setBinary(new DataHandler((DataSource) new ByteArrayDataSource(bin,
                    parameters.getBinary().getContentType())));
            return response;
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
