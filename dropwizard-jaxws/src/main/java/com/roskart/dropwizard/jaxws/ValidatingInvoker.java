package com.roskart.dropwizard.jaxws;

import io.dropwizard.validation.Validated;
import io.dropwizard.validation.ConstraintViolations;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.Invoker;

import jakarta.validation.Validator;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.groups.Default;
import jakarta.xml.ws.AsyncHandler;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Wraps underlying CXF invoker and performs validation of the service operation parameters.
 * @see io.dropwizard.jersey.jackson.JacksonMessageBodyProvider
 */
public class ValidatingInvoker extends AbstractInvoker {

    private static final Class<?>[] DEFAULT_GROUP_ARRAY = new Class<?>[]{ Default.class };

    private Validator validator;

    public ValidatingInvoker(Invoker underlying, Validator validator) {
        super(underlying);
        this.validator = validator;
    }

    @Override
    public Object invoke(Exchange exchange, Object o) {

        // get annotations declared on parameters
        Annotation[][] parameterAnnotations = this.getTargetMethod(exchange).getParameterAnnotations();

        /* Get actual parameter list start (code copied from org.apache.cxf.service.invoker.AbstractInvoker.invoke) */
        List<Object> params = null;
        if (o instanceof List) {
            params = CastUtils.cast((List<?>) o);
        } else if (o != null) {
            params = new MessageContentsList(o);
        }
        /* Get actual parameter list end */

        // validate each parameter in the list
        if (params != null) {
            int i = 0;
            try {
                for (Object parameter : params) {
                    if(parameter == null || !AsyncHandler.class.isAssignableFrom(parameter.getClass())) {
                        validate(parameterAnnotations[i++], parameter);
                    }
                }
            }
            catch (ValidationException ve) {
                // Prevent CXF PhaseInterceptorChain to log complete stack trace (happens because ValidationException
                // extends RuntimeException). Only error message with INFO level will be logged.
                exchange.getInMessage().put(FaultMode.class, FaultMode.CHECKED_APPLICATION_FAULT);
                throw ve;
            }
        }

        return underlying.invoke(exchange, o);
    }

    /**
     * Copied and modified from com.yammer.dropwizard.jersey.jackson#JacksonMessageBodyProvider.validate()
     * Notes on Hibernate Validator:
     * - when validating object graphs, null references are ignored.
     * - from version 5 on, Hibernate Validator throws IllegalArgumentException instead of ValidationException
     *   for null parameter values:
     *   java.lang.IllegalArgumentException: HV000116: The object to be validated must not be null.
     */
    private Object validate(Annotation[] annotations, Object value) {
        final Class<?>[] classes = findValidationGroups(annotations);

        if (classes != null) {
            final Collection<String> errors = ConstraintViolations.format(
                    validator.validate(value, classes));

            if (!errors.isEmpty()) {
                String message = "\n";
                for (String error : errors) {
                    message += "    " + error + "\n";
                }
                throw new ValidationException(message);
            }
        }

        return value;
    }

    /**
     * Copied from com.yammer.dropwizard.jersey.jackson.JacksonMessageBodyProvider#findValidationGroups()
     */
    private Class<?>[] findValidationGroups(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Valid.class) {
                return DEFAULT_GROUP_ARRAY;
            } else if (annotation.annotationType() == Validated.class) {
                return  ((Validated) annotation).value();
            }
        }
        return null;
    }
}
