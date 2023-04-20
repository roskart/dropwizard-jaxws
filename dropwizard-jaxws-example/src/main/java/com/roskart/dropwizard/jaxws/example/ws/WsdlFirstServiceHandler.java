package com.roskart.dropwizard.jaxws.example.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Set;

public class WsdlFirstServiceHandler implements SOAPHandler<SOAPMessageContext> {

    Logger log = LoggerFactory.getLogger(WsdlFirstServiceHandler.class);

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
            log.info("WsdlFirstService server handler - outbound");
        }
        else {
            log.info("WsdlFirstService server handler - inbound");
        }

        return true;
    }

}
