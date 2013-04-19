package com.roskart.dropwizard.jaxws;

import com.google.common.collect.ImmutableMap;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Metered;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Timer;
import org.apache.cxf.service.invoker.Invoker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides factory method for creating instrumented CXF invoker chain.
 * @see com.roskart.dropwizard.jaxws.InstrumentedInvokers
 * @see com.yammer.metrics.jersey.InstrumentedResourceMethodDispatchProvider
 */
public class InstrumentedInvokerFactory {

    /**
     * Factory method for TimedInvoker.
     */
    private Invoker timed(Invoker invoker, List<Method> timedMethods) {

        ImmutableMap.Builder<String, Timer> timers = new ImmutableMap.Builder<String, Timer>();

        for (Method m : timedMethods) {

            Timed annotation = m.getAnnotation(Timed.class);

            MetricName metricname = new MetricName(
                    MetricName.chooseGroup(annotation.group(), m.getDeclaringClass()),
                    MetricName.chooseType(annotation.type(), m.getDeclaringClass()),
                    MetricName.chooseName(annotation.name(), m));

            Timer timer = Metrics.newTimer(metricname,
                    annotation.durationUnit() == null ? TimeUnit.MILLISECONDS : annotation.durationUnit(),
                    annotation.rateUnit() == null ? TimeUnit.SECONDS : annotation.rateUnit());

            timers.put(m.getName(), timer);
        }

        return new InstrumentedInvokers.TimedInvoker(invoker, timers.build());
    }

    /**
     * Factory method for MeteredInvoker.
     */
    private Invoker metered(Invoker invoker, List<Method> meteredMethods) {

        ImmutableMap.Builder<String, Meter> meters = new ImmutableMap.Builder<String, Meter>();

        for (Method m : meteredMethods) {

            Metered annotation = m.getAnnotation(Metered.class);

            MetricName metricname = new MetricName(
                    MetricName.chooseGroup(annotation.group(), m.getDeclaringClass()),
                    MetricName.chooseType(annotation.type(), m.getDeclaringClass()),
                    MetricName.chooseName(annotation.name(), m));

            Meter meter = Metrics.newMeter(metricname,
                    annotation.eventType() == null ? "requests" : annotation.eventType(),
                    annotation.rateUnit() == null ? TimeUnit.SECONDS : annotation.rateUnit());

            meters.put(m.getName(), meter);
        }

        return new InstrumentedInvokers.MeteredInvoker(invoker, meters.build());
    }

    /**
     * Factory method for ExceptionMeteredInvoker.
     */
    private Invoker exceptionMetered(Invoker invoker, List<Method> meteredMethods) {

        ImmutableMap.Builder<String, InstrumentedInvokers.ExceptionMeter> meters =
                new ImmutableMap.Builder<String, InstrumentedInvokers.ExceptionMeter>();

        for (Method m : meteredMethods) {

            ExceptionMetered annotation = m.getAnnotation(ExceptionMetered.class);

            MetricName metricname = new MetricName(
                    MetricName.chooseGroup(annotation.group(), m.getDeclaringClass()),
                    MetricName.chooseType(annotation.type(), m.getDeclaringClass()),
                    annotation.name() == null || annotation.name().equals("") ?
                        m.getName() + ExceptionMetered.DEFAULT_NAME_SUFFIX : annotation.name());

            Meter meter = Metrics.newMeter(metricname,
                    annotation.eventType() == null ? "requests" : annotation.eventType(),
                    annotation.rateUnit() == null ? TimeUnit.SECONDS : annotation.rateUnit());

            meters.put(m.getName(), new InstrumentedInvokers.ExceptionMeter(meter, annotation.cause()));
        }

        return new InstrumentedInvokers.ExceptionMeteredInvoker(invoker, meters.build());
    }

    /**
     * Factory method for creating instrumented invoker chain.
     */
    public Invoker create(Object service, Invoker rootInvoker) {

        List<Method> timedmethods = new ArrayList<Method>();
        List<Method> meteredmethods = new ArrayList<Method>();
        List<Method> exceptionmeteredmethods = new ArrayList<Method>();

        for (Method m : service.getClass().getMethods()) {

            if (m.isAnnotationPresent(Timed.class)) {
                timedmethods.add(m);
            }

            if (m.isAnnotationPresent(Metered.class)) {
                meteredmethods.add(m);
            }

            if (m.isAnnotationPresent(ExceptionMetered.class)) {
                exceptionmeteredmethods.add(m);
            }
        }

        Invoker invoker = rootInvoker;

        if (timedmethods.size() > 0) {
            invoker = this.timed(invoker, timedmethods);
        }

        if (meteredmethods.size() > 0) {
            invoker = this.metered(invoker, meteredmethods);
        }

        if (exceptionmeteredmethods.size() > 0) {
            invoker = this.exceptionMetered(invoker, exceptionmeteredmethods);
        }

        return invoker;
    }

}
