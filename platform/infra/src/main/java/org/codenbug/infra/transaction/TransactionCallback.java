package org.codenbug.infra.transaction;

@FunctionalInterface
public interface TransactionCallback<T> {
	T execute();
}