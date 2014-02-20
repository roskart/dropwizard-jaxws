package com.roskart.dropwizard.jaxws;

import com.google.common.collect.ImmutableMap;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

/**
 * Provides instrumented CXF invoker implementations.
 * @see com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchProvider
 */
public class InstrumentedInvokers {

    /**
     * Wraps underlying invoker and manages timers for methods annotated with @Timed.
     */
    public static class TimedInvoker extends AbstractInvoker {

        private ImmutableMap<String, Timer> timers;

        public TimedInvoker(Invoker underlying, ImmutableMap<String, Timer> timers) {
            super(underlying);
            this.timers = timers;
        }

        public Object invoke(Exchange exchange, Object o) {

            Object result = null;
            String methodname = this.getTargetMethod(exchange).getName();

            if (timers.containsKey(methodname)) {
                Timer timer = timers.get(methodname);
                final Timer.Context context = timer.time();
                try {
                    result = this.underlying.invoke(exchange, o);
                }
                finally {
                    context.stop();
                }
            }
            else {
                result = this.underlying.invoke(exchange, o);
            }
            return result;
        }
    }

    /**
     * Wraps underlying invoker and manages meters for methods annotated with @Metered.
     */
    public static class MeteredInvoker extends AbstractInvoker {

        private ImmutableMap<String, Meter> meters;

        public MeteredInvoker(Invoker underlying, ImmutableMap<String, Meter> meters) {
            super(underlying);
            this.meters = meters;
        }

        public Object invoke(Exchange exchange, Object o) {

            Object result;
            String methodname = this.getTargetMethod(exchange).getName();

            if (meters.containsKey(methodname)) {
                Meter meter = meters.get(methodname);
                meter.mark();
            }
            result = this.underlying.invoke(exchange, o);
            return result;
        }
    }

    public static class ExceptionMeter {
        private Meter meter;
        private Class<? extends Throwable> exceptionClass;
        public ExceptionMeter(Meter meter, Class<? extends Throwable> exceptionClass) {
            this.meter = meter;
            this.exceptionClass = exceptionClass;
        }
        public Meter getMeter() {
            return meter;
        }
        public Class<? extends Throwable> getExceptionClass() {
            return exceptionClass;
        }
    }

    /**
     * Wraps underlying invoker and manages meters for methods annotated with @ExceptionMetered.
     */
    public static class ExceptionMeteredInvoker extends AbstractInvoker {

        private ImmutableMap<String, ExceptionMeter> meters;

        public ExceptionMeteredInvoker(Invoker underlying, ImmutableMap<String, ExceptionMeter> meters) {
            super(underlying);
            this.meters = meters;
        }

        public Object invoke(Exchange exchange, Object o) {

            Object result;
            String methodname = this.getTargetMethod(exchange).getName();

            try {
                result = this.underlying.invoke(exchange, o);
                return result;
            }
            catch (Exception e) {

                if (meters.containsKey(methodname)) {
                    ExceptionMeter meter = meters.get(methodname);
                    if (meter.getExceptionClass().isAssignableFrom(e.getClass()) ||
                            (e.getCause() != null &&
                                    meter.getExceptionClass().isAssignableFrom(e.getCause().getClass()))) {
                        meter.getMeter().mark();
                    }
                }
                this.<RuntimeException>rethrow(e); // unchecked rethrow
                return null; // avoid compiler warning
            }
        }

    }

}
