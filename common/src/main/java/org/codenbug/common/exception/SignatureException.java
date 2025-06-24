package org.codenbug.common.exception;

public class SignatureException extends JwtException {
	public SignatureException(){
		super("401", "Invalid Signature");
	}
}
