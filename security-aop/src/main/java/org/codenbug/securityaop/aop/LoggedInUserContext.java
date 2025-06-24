package org.codenbug.securityaop.aop;

import java.io.IOException;

public class LoggedInUserContext implements AutoCloseable {
	private static final ThreadLocal<UserSecurityToken> storage = new ThreadLocal<>();

	public static LoggedInUserContext open(UserSecurityToken token){
		return new LoggedInUserContext(token);
	}
	private LoggedInUserContext(UserSecurityToken token) {
		storage.set(token);
	}

	public static UserSecurityToken get() {
		return storage.get();
	}

	public static void clear() {
		storage.remove();
	}

	@Override
	public void close() throws IOException {
		clear();
	}
}
