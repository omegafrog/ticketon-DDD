package common;


public class ValidationError extends RuntimeException {
	public ValidationError(String msg){
		super(msg);
	}
}
