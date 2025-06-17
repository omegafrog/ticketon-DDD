package org.codenbug.auth.domain;

import lombok.Getter;

@Getter
public class TokenInfo {
	private AccessToken accessToken;
	private RefreshToken refreshToken;

	protected TokenInfo(){}
	public TokenInfo(AccessToken accessToken, RefreshToken refreshToken){
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
}
