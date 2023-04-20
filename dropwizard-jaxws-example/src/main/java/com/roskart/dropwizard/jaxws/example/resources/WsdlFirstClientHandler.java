package com.roskart.dropwizard.jaxws.example.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Set;

/**
 * JAX-WS client handler used when WsdlFirstService is invoked.
 * @see AccessWsdlFirstServiceResource
 */
public class WsdlFirstClientHandler implements SOAPHandler<SOAPMessageContext> {

    Logger log = LoggerFactory.getLogger(WsdlFirstClientHandler.class);

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public void close(MessageContext messageContext) {
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {

        Boolean outbound = (Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outbound) {
            log.info("WsdlFirstService client handler - outbound");
        }
        else {
            log.info("WsdlFirstService client handler - inbound");
        }

        return true;
    }

}
