package com.roskart.dropwizard.jaxws;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;

/**
 * Wraps underlying invoker in a Hibernate session.
 * @see io.dropwizard.hibernate.UnitOfWorkRequestDispatcher
 */
public class UnitOfWorkInvoker extends AbstractInvoker {

    private final SessionFactory sessionFactory;
    ImmutableMap<String, UnitOfWork> unitOfWorkMethods;

    public UnitOfWorkInvoker(Invoker underlying, ImmutableMap<String, UnitOfWork> unitOfWorkMethods,
                             SessionFactory sessionFactory) {
        super(underlying);
        this.unitOfWorkMethods = unitOfWorkMethods;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Object invoke(Exchange exchange, Object o) {

        Object result;
        String methodname = this.getTargetMethod(exchange).getName();

        if (unitOfWorkMethods.containsKey(methodname)) {

            final Session session = sessionFactory.openSession();
            UnitOfWork unitOfWork = unitOfWorkMethods.get(methodname);

            try {
                configureSession(session, unitOfWork);
                ManagedSessionContext.bind(session);
                beginTransaction(session, unitOfWork);
                try {
                    result = underlying.invoke(exchange, o);
                    commitTransaction(session, unitOfWork);
                    return result;
                } catch (Exception e) {
                    rollbackTransaction(session, unitOfWork);
                    this.<RuntimeException>rethrow(e); // unchecked rethrow
                    return null; // avoid compiler warning
                }
            } finally {
                session.close();
                ManagedSessionContext.unbind(sessionFactory);
            }
        }
        else {
            return underlying.invoke(exchange, o);
        }
    }

    /**
     * Copied from com.yammer.dropwizard.hibernate.UnitOfWorkRequestDispatcher#beginTransaction()
     */
    private void beginTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            session.beginTransaction();
        }
    }

    /**
     * Copied from com.yammer.dropwizard.hibernate.UnitOfWorkRequestDispatcher#configureSession()
     */
    private void configureSession(Session session, UnitOfWork unitOfWork) {
        session.setDefaultReadOnly(unitOfWork.readOnly());
        session.setCacheMode(unitOfWork.cacheMode());
        session.setFlushMode(unitOfWork.flushMode());
    }

    /**
     * Copied from com.yammer.dropwizard.hibernate.UnitOfWorkRequestDispatcher#rollbackTransaction()
     */
    private void rollbackTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            final Transaction txn = session.getTransaction();
            if (txn != null && txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Copied from com.yammer.dropwizard.hibernate.UnitOfWorkRequestDispatcher#commitTransaction()
     */
    private void commitTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            final Transaction txn = session.getTransaction();
            if (txn != null && txn.isActive()) {
                txn.commit();
            }
        }
    }

}
