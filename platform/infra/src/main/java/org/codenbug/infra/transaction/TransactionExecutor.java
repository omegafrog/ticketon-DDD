package org.codenbug.infra.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TransactionExecutor {

	public static <T> T executeInTransaction(PlatformTransactionManager manager, TransactionCallback<T> callback) {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		TransactionStatus status = manager.getTransaction(def);
		try {
			T result = callback.execute();
			manager.commit(status);
			return result;
		} catch (RuntimeException e) {
			manager.rollback(status);
			throw e;
		}
	}
}