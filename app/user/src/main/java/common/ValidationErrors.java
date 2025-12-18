package common;

import java.util.ArrayList;
import java.util.List;

public class ValidationErrors extends RuntimeException {
	private List<ValidationError> errors = new ArrayList<>();

	public ValidationErrors(List<ValidationError> errors) {
		this.errors = errors;
	}
}
