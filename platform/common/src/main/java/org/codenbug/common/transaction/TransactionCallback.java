package org.codenbug.common.transaction;

/**
 * 트랜잭션 콜백 인터페이스
 */
@FunctionalInterface
public interface TransactionCallback<T> {
	T execute();
}

