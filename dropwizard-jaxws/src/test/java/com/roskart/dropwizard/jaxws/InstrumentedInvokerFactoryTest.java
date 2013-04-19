package com.roskart.dropwizard.jaxws;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Metered;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

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

        invokerBuilder = new InstrumentedInvokerFactory();
        instrumentedService = new InstrumentedService();
    }

    @Test
    public void noAnnotation() {

        Timer timer = Metrics.newTimer(InstrumentedService.class, "timed");
        Meter meter = Metrics.newMeter(InstrumentedService.class, "metered", "", TimeUnit.SECONDS);

        long oldtimervalue = timer.count();
        long oldmetervalue = meter.count();

        Invoker invoker = invokerBuilder.create(instrumentedService, new FooInvoker());
        this.setTargetMethod(exchange, "foo"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("fooReturn", result);

        assertThat(timer.count(), is(oldtimervalue));
        assertThat(meter.count(), is(oldmetervalue));
    }

    @Test
    public void meteredAnnotation() {

        Timer timer = Metrics.newTimer(InstrumentedService.class, "timed");
        Meter meter = Metrics.newMeter(InstrumentedService.class, "metered", "", TimeUnit.SECONDS);

        long oldtimervalue = timer.count();
        long oldmetervalue = meter.count();

        Invoker invoker = invokerBuilder.create(instrumentedService, new MeteredInvoker());
        this.setTargetMethod(exchange, "metered"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("meteredReturn", result);

        assertThat(timer.count(), is(oldtimervalue));
        assertThat(meter.count(), is(1 + oldmetervalue));
    }

    @Test
    public void timedAnnotation() {

        Timer timer = Metrics.newTimer(InstrumentedService.class, "timed");
        Meter meter = Metrics.newMeter(InstrumentedService.class, "metered", "", TimeUnit.SECONDS);

        long oldtimervalue = timer.count();
        long oldmetervalue = meter.count();

        Invoker invoker = invokerBuilder.create(instrumentedService, new TimedInvoker());
        this.setTargetMethod(exchange, "timed"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("timedReturn", result);

        assertThat(timer.count(), is(1 + oldtimervalue));
        assertThat(meter.count(), is(oldmetervalue));
    }

    @Test
    public void exceptionMeteredAnnotation() {

        Timer timer = Metrics.newTimer(InstrumentedService.class, "timed");
        Meter meter = Metrics.newMeter(InstrumentedService.class, "metered", "", TimeUnit.SECONDS);
        Meter exceptionmeter = Metrics.newMeter(InstrumentedService.class, "exceptionMeteredExceptions", "", TimeUnit.SECONDS);

        long oldtimervalue = timer.count();
        long oldmetervalue = meter.count();
        long oldexceptionmetervalue = exceptionmeter.count();

        // Invoke InstrumentedResource.exceptionMetered without exception beeing thrown

        Invoker invoker = invokerBuilder.create(instrumentedService, new ExceptionMeteredInvoker(false));
        this.setTargetMethod(exchange, "exceptionMetered", boolean.class); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("exceptionMeteredReturn", result);

        assertThat(timer.count(), is(oldtimervalue));
        assertThat(meter.count(), is(oldmetervalue));
        assertThat(exceptionmeter.count(), is(oldexceptionmetervalue));

        // Invoke InstrumentedResource.exceptionMetered with exception beeing thrown

        invoker = invokerBuilder.create(instrumentedService, new ExceptionMeteredInvoker(true));

        try {
            invoker.invoke(exchange, null);
            fail("Exception shall be thrown here");
        }
        catch (Exception e) {
            assertThat(e, is(instanceOf(RuntimeException.class)));
        }

        assertThat(timer.count(), is(oldtimervalue));
        assertThat(meter.count(), is(oldmetervalue));
        assertThat(exceptionmeter.count(), is(1 + oldexceptionmetervalue));

    }

}
