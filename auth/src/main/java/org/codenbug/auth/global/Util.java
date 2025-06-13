package org.codenbug.auth.global;

import java.net.http.HttpHeaders;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import org.codenbug.auth.domain.AccessToken;
import org.codenbug.auth.domain.RefreshToken;
import org.codenbug.auth.domain.Role;
import org.codenbug.auth.domain.UserId;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;

public class Util {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	public static RefreshToken parseRefreshToken(Cookie[] cookies) {
		String refreshTokenValue = Arrays.stream(cookies)
			.filter(item -> item.getName().equals(REFRESH_TOKEN_COOKIE_NAME
			))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Cannot find refresh token from cookies"))
			.getValue();

		return new RefreshToken(refreshTokenValue);
	}

	/**
	 * Get Access token key-value string from Authorization header
	 * @param authHeader http header value that has "Authorization" as  key
	 * @return {@link AccessToken}
	 */
	public static AccessToken parseAccessToken(String authHeader) {
		String[] s = authHeader.split(" ");
		return new AccessToken(s[1], s[0]);
	}

	public static Claims getClaims(String jwt, PublicKey publicKey) {
		return (Claims)Jwts.parser().verifyWith(publicKey).build()
			.parse(jwt)
			.getPayload();
	}

	public static Role getRole(AccessToken jwt, PublicKey publicKey) {
		Claims claims = getClaims(jwt.getValue(), publicKey);
		return claims.get("role", Role.class);
	}

	public static UserId getUserId(AccessToken jwt, PublicKey publicKey) {
		Claims claims = getClaims(jwt.getValue(), publicKey);
		return new UserId(claims.get("userId", String.class));
	}

	public static void hasTokenExpired(AccessToken accessToken) {
		getExpiration(accessToken.getValue());
	}

	private static void getExpiration(String value) {
		getClaims(value, null);
	}

	public static class Key {
		public static PublicKey getPublicKeyFromBase64(String base64PublicKey) {
			byte[] decoded = Base64.getDecoder().decode(base64PublicKey);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
			try {
				KeyFactory kf = KeyFactory.getInstance("RSA");        // 알고리즘(RSA, EC 등)과 맞춰주세요
				return kf.generatePublic(spec);
			} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
