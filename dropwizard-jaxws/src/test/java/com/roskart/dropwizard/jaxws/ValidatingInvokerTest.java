package com.roskart.dropwizard.jaxws;

import io.dropwizard.validation.Validated;
import io.dropwizard.validation.ValidationMethod;
import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.Before;
import org.junit.Test;

import jakarta.validation.Validation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ValidatingInvokerTest {

    ValidatingInvoker invoker;
    Invoker underlying;
    Exchange exchange;

    class ChildParam {
        @NotEmpty
        private String foo;
        public ChildParam(String foo) {
            this.foo = foo;
        }
        @ValidationMethod(message="foo may not be 'John'")
        public boolean isNotJohn() {
            return !("John".equals(foo));
        }
    }

    class RootParam1 {
        @Valid
        private ChildParam child;
        public RootParam1(ChildParam childParam) {
            this.child = childParam;
        }
    }

    class RootParam2 {
        @NotEmpty
        private String foo;
        public RootParam2(String foo) {
            this.foo = foo;
        }
    }

    class DummyService {
        public void noParams() {
        }
        public void noValidation(RootParam1 rootParam1, RootParam2 rootParam2) {
        }
        public void withValidation(@Valid RootParam1 rootParam1, @Valid RootParam2 rootParam2) {
        }
        public void withDropwizardValidation(@Validated() String foo) {
        }
        @UseAsyncMethod
        public void asyncMethod(String foo) {
        }
        public void asyncMethodAsync(String foo, AsyncHandler asyncHandler) {
        }
    }

    @Before
    public void setup() {
        underlying = mock(Invoker.class);
        invoker = new ValidatingInvoker(underlying, Validation.buildDefaultValidatorFactory().getValidator());
        exchange = mock(Exchange.class);
        when(exchange.getInMessage()).thenReturn(mock(Message.class));
        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(boi);
        OperationInfo oi = mock(OperationInfo.class);
        when(boi.getOperationInfo()).thenReturn(oi);
    }

    /**
     * Utility method that mimics runtime CXF behaviour. Enables AbstractInvoker.getTargetMethod to work properly
     * during the test.
     */
    private void setTargetMethod(Exchange exchange, String methodName, Class<?>... parameterTypes) {
        try {
            OperationInfo oi = exchange.getBindingOperationInfo().getOperationInfo();
            when(oi.getProperty(Method.class.getName()))
                    .thenReturn(DummyService.class.getMethod(methodName, parameterTypes));
        }
        catch (Exception e) {
            fail("setTargetMethod failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Test
    public void invokeWithoutParams() {
        setTargetMethod(exchange, "noParams");
        invoker.invoke(exchange, null);
        verify(underlying).invoke(exchange, null);
    }

    @Test
    public void invokeWithoutValidation() {
        setTargetMethod(exchange, "noValidation", RootParam1.class, RootParam2.class);

        List<Object> params = Arrays.asList(null, null);
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);

        params = Arrays.asList(new RootParam1(null), new RootParam2(null));
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);
    }

    @Test
    public void invokeWithAsycHandler() {
        setTargetMethod(exchange, "asyncMethod", String.class);

        List<Object> params = Arrays.<Object>asList(null, new AsyncHandler(){
            @Override
            public void handleResponse(Response res) {

            }
        });
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);

        params = Arrays.asList("foo", new AsyncHandler(){
            @Override
            public void handleResponse(Response res) {

            }
        });
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);
    }

    @Test
    public void invokeWithValidation() {

        setTargetMethod(exchange, "withValidation", RootParam1.class, RootParam2.class);

        List<Object> params = Arrays.asList(new RootParam1(new ChildParam("")), new RootParam2("ok"));

        try {
            invoker.invoke(exchange, params);
            fail();
        }
        catch(Exception e) {
            assertThat(e, is(instanceOf(ValidationException.class)));
        }

        params = Arrays.asList(new RootParam1(new ChildParam("")), new RootParam2("ok"));
        try {
            invoker.invoke(exchange, params);
            fail();
        }
        catch(Exception e) {
            assertThat(e, is(instanceOf(ValidationException.class)));
        }

        params = Arrays.asList(new RootParam1(new ChildParam("John")), new RootParam2("ok"));
        try {
            invoker.invoke(exchange, params);
            fail();
        }
        catch(Exception e) {
            assertThat(e, is(instanceOf(ValidationException.class)));
        }

        verifyNoMoreInteractions(underlying);

        params = Arrays.asList(new RootParam1(new ChildParam("ok")), new RootParam2("ok"));
        invoker.invoke(exchange, params);

        verify(underlying).invoke(exchange, params);
    }
}
