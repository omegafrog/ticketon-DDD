package org.codenbug.user.domain;

public record MaskedUserIdentity(String maskedEmail, String maskedName) {
	public static MaskedUserIdentity of(String email, String name) {
		return new MaskedUserIdentity(maskEmail(email), maskName(name));
	}

	private static String maskEmail(String email) {
		if (email == null || email.isBlank() || !email.contains("@")) {
			throw new IllegalArgumentException("Email is invalid.");
		}
		String[] parts = email.split("@", 2);
		String local = parts[0];
		String domain = parts[1];
		if (local.isBlank() || domain.isBlank()) {
			throw new IllegalArgumentException("Email is invalid.");
		}
		if (local.length() <= 4) {
			return local.charAt(0) + "***@" + domain;
		}
		return local.substring(0, 2) + "***" + local.substring(local.length() - 2) + "@" + domain;
	}

	private static String maskName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Name is invalid.");
		}
		if (name.length() <= 2) {
			return name;
		}
		return name.substring(0, 2);
	}
}
