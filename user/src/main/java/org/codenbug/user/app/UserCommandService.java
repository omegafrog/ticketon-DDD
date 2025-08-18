package org.codenbug.user.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.user.domain.GenerateUserIdService;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.classify.PatternMatcher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PatternMatchUtils;

import common.ValidationError;
import common.ValidationErrors;

@Service
public class UserCommandService {

	private final UserRepository userRepository;
	private final GenerateUserIdService idService;

	private final Pattern pattern = Pattern.compile("^01([0|1|6|7|8|9])-\\d{3,4}-\\d{4}$");


	public UserCommandService(UserRepository userRepository, ApplicationEventPublisher publisher,
		GenerateUserIdService idService) {
		this.userRepository = userRepository;
		this.idService = idService;
	}

	@Transactional
	public UserId register(RegisterRequest request) {

		validateRequest(request);

		return userRepository.save(
			new User(idService, request.getName(), Sex.valueOf(request.getSex()),
				request.getPhoneNum(), request.getLocation(),
				request.getAge(), request.getSecurityUserId()));
	}

	private void validateRequest(RegisterRequest request) {
		List<ValidationError> errors = new ArrayList<>();
		if(request.getName() == null || request.getName().isEmpty()){
			errors.add(new ValidationError("Name must not be null or empty."));
		}
		if (request.getLocation() == null || request.getLocation().isEmpty()) {
			errors.add(new ValidationError("Location must not be null or empty."));
		}
		if(request.getSecurityUserId() == null){
			errors.add(new ValidationError("SecurityUserId must not be null"));
		}
		if (request.getAge() == null || request.getAge() < 0) {
			errors.add(new ValidationError("Age must not be null or bigger than -1"));
		}
		if (request.getSex() == null) {
			errors.add(new ValidationError("Sex must not be null."));
		}
		if(!request.getSex().equals(Sex.MALE.name()) && !request.getSex().equals(Sex.FEMALE.name())
			&& !request.getSex().equals(Sex.ETC.name())){
			errors.add(new ValidationError("Sex must be \"MALE\" or \"FEMALE\" or \"ETC\""));
		}
		if(!pattern.matcher(request.getPhoneNum()).matches()){
			errors.add(new ValidationError("Phone number must match pattern ^01([0|1|6|7|8|9])-\\d{3,4}-\\d{4}$."));
		}
		if(!errors.isEmpty()){
			throw new ValidationErrors(errors);
		}
	}

	@Transactional
	public void update( UpdateRequest request){

		User user = userRepository.findUser(request.userId());
		if(!LoggedInUserContext.get().getUserId().equals(user.getUserId().getValue())){
			throw new AccessDeniedException("Cannot update other user's information.");
		}

		user.update(request.name(), request.age(), request.location(), request.phoneNum());
	}

	public void cancelUserRegistration(String userId) {
		userRepository.delete(userId);
	}
}
