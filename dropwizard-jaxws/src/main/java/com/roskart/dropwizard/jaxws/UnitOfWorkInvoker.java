package com.roskart.dropwizard.jaxws;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Wraps underlying invoker in a Hibernate session. Code in this class is based on Dropwizard's UnitOfWorkApplication
 * listener and UnitOfWorkAspect. We don't use UnitOfWorkAspect here because it is declared package private.
 * @see io.dropwizard.hibernate.UnitOfWorkAspect
 * @see io.dropwizard.hibernate.UnitOfWorkApplicationListener
 * @see io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory
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
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#beginTransaction()
     */
    private void beginTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            session.beginTransaction();
        }
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#configureSession()
     */
    private void configureSession(Session session, UnitOfWork unitOfWork) {
        session.setDefaultReadOnly(unitOfWork.readOnly());
        session.setCacheMode(unitOfWork.cacheMode());
        session.setHibernateFlushMode(unitOfWork.flushMode());
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#rollbackTransaction()
     */
    private void rollbackTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            final Transaction txn = session.getTransaction();
            if (txn != null && txn.getStatus().equals(TransactionStatus.ACTIVE)) {
                txn.rollback();
            }
        }
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#commitTransaction()
     */
    private void commitTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            final Transaction txn = session.getTransaction();
            if (txn != null && txn.getStatus().equals(TransactionStatus.ACTIVE)) {
                txn.commit();
            }
        }
    }

}
