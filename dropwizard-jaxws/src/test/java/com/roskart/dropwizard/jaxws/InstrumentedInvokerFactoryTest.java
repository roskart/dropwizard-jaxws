package com.roskart.dropwizard.jaxws;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

public class InstrumentedInvokerFactoryTest {

    // Test service implementation
    class InstrumentedService {

        public String foo() {
            return "fooReturn";
        }

        @Metered
        public String metered() {
            return "meteredReturn";
        }

        @Timed
        public String timed() {
            return "timedReturn";
        }

        @ExceptionMetered
        public String exceptionMetered(boolean doThrow) {
            if (doThrow) {
                throw new RuntimeException("Runtime exception occured");
            }
            else {
                return "exceptionMeteredReturn";
            }
        }
    }

    MetricRegistry testMetricRegistry;
    MetricRegistry mockMetricRegistry;
    InstrumentedInvokerFactory invokerBuilder;
    InstrumentedService instrumentedService;
    // CXF Exchange contains message exchange and is used by Invoker to obtain invoked method name
    Exchange exchange;

    /* Invokers that invoke test service implementation */

    class FooInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.foo();
        }
    }

    class MeteredInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.metered();
        }
    }

    class TimedInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.timed();
        }
    }

    public class ExceptionMeteredInvoker implements Invoker {
        private boolean doThrow;
        public ExceptionMeteredInvoker(boolean doThrow) {
            this.doThrow = doThrow;
        }
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.exceptionMetered(doThrow);
        }
    }

    /**
     * Utility method that mimics runtime CXF behaviour. Enables AbstractInvoker.getTargetMethod to work properly
     * during the test.
     */
    private void setTargetMethod(Exchange exchange, String methodName, Class<?>... parameterTypes) {

        try {
            OperationInfo oi = exchange.getBindingOperationInfo().getOperationInfo();
            when(oi.getProperty(Method.class.getName()))
                    .thenReturn(InstrumentedService.class.getMethod(methodName, parameterTypes));
        }
        catch (Exception e) {
            fail("setTargetMethod failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Before
    public void setUp() {
        exchange = mock(Exchange.class);

        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(boi);

        OperationInfo oi = mock(OperationInfo.class);
        when(boi.getOperationInfo()).thenReturn(oi);

        testMetricRegistry = new MetricRegistry();
        mockMetricRegistry = mock(MetricRegistry.class);

        invokerBuilder = new InstrumentedInvokerFactory(mockMetricRegistry);
        instrumentedService = new InstrumentedService();
    }

    @Test
    public void noAnnotation() {

        Timer timer = testMetricRegistry.timer("timed");
        Meter meter = testMetricRegistry.meter("metered");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(anyString())).thenReturn(meter);

        long oldtimervalue = timer.getCount();
        long oldmetervalue = meter.getCount();

        Invoker invoker = invokerBuilder.create(instrumentedService, new FooInvoker());
        this.setTargetMethod(exchange, "foo"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("fooReturn", result);

        assertThat(timer.getCount(), is(oldtimervalue));
        assertThat(meter.getCount(), is(oldmetervalue));
    }

    @Test
    public void meteredAnnotation() {

        Timer timer = testMetricRegistry.timer("timed");
        Meter meter = testMetricRegistry.meter("metered");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(anyString())).thenReturn(meter);

        long oldtimervalue = timer.getCount();
        long oldmetervalue = meter.getCount();

        Invoker invoker = invokerBuilder.create(instrumentedService, new MeteredInvoker());
        this.setTargetMethod(exchange, "metered"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("meteredReturn", result);

        assertThat(timer.getCount(), is(oldtimervalue));
        assertThat(meter.getCount(), is(1 + oldmetervalue));
    }

    @Test
    public void timedAnnotation() {

        Timer timer = testMetricRegistry.timer("timed");
        Meter meter = testMetricRegistry.meter("metered");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(anyString())).thenReturn(meter);

        long oldtimervalue = timer.getCount();
        long oldmetervalue = meter.getCount();

        Invoker invoker = invokerBuilder.create(instrumentedService, new TimedInvoker());
        this.setTargetMethod(exchange, "timed"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("timedReturn", result);

        assertThat(timer.getCount(), is(1 + oldtimervalue));
        assertThat(meter.getCount(), is(oldmetervalue));
    }

    @Test
    public void exceptionMeteredAnnotation() {

        Timer timer = testMetricRegistry.timer("timed");
        Meter meter = testMetricRegistry.meter("metered");
        Meter exceptionmeter = testMetricRegistry.meter("exceptionMeteredExceptions");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(contains("metered"))).thenReturn(meter);
        when(mockMetricRegistry.meter(contains("exceptionMetered"))).thenReturn(exceptionmeter);

        long oldtimervalue = timer.getCount();
        long oldmetervalue = meter.getCount();
        long oldexceptionmetervalue = exceptionmeter.getCount();

        // Invoke InstrumentedResource.exceptionMetered without exception beeing thrown

        Invoker invoker = invokerBuilder.create(instrumentedService, new ExceptionMeteredInvoker(false));
        this.setTargetMethod(exchange, "exceptionMetered", boolean.class); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("exceptionMeteredReturn", result);

        assertThat(timer.getCount(), is(oldtimervalue));
        assertThat(meter.getCount(), is(oldmetervalue));
        assertThat(exceptionmeter.getCount(), is(oldexceptionmetervalue));

        // Invoke InstrumentedResource.exceptionMetered with exception beeing thrown

        invoker = invokerBuilder.create(instrumentedService, new ExceptionMeteredInvoker(true));

        try {
            invoker.invoke(exchange, null);
            fail("Exception shall be thrown here");
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(RuntimeException.class)));
        }

        assertThat(timer.getCount(), is(oldtimervalue));
        assertThat(meter.getCount(), is(oldmetervalue));
        assertThat(exceptionmeter.getCount(), is(1 + oldexceptionmetervalue));

    }

}
