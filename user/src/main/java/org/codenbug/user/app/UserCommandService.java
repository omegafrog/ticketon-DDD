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
import org.codenbug.user.ui.RegisterValidationRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    validateRegisterFields(request.getName(), request.getAge(), request.getSex(),
        request.getPhoneNum(), request.getLocation());

    if (request.getSecurityUserId() == null) {
      throw new ValidationErrors(List.of(new ValidationError("SecurityUserId must not be null")));
    }

    return userRepository.save(
        new User(idService, request.getName(), Sex.valueOf(request.getSex()), request.getPhoneNum(),
            request.getLocation(), request.getAge(), request.getSecurityUserId()));
  }

  public void validateRegisterInputs(RegisterValidationRequest request) {
    validateRegisterFields(request.getName(), request.getAge(), request.getSex(),
        request.getPhoneNum(), request.getLocation());
  }

  private void validateRegisterFields(String name, Integer age, String sex, String phoneNum,
      String location) {
    List<ValidationError> errors = new ArrayList<>();
    if (name == null || name.isEmpty()) {
      errors.add(new ValidationError("Name must not be null or empty."));
    }
    if (location == null || location.isEmpty()) {
      errors.add(new ValidationError("Location must not be null or empty."));
    }
    if (age == null || age < 0) {
      errors.add(new ValidationError("Age must not be null or bigger than -1"));
    }
    if (sex == null) {
      errors.add(new ValidationError("Sex must not be null."));
    }
    if (sex != null && !sex.equals(Sex.MALE.name()) && !sex.equals(Sex.FEMALE.name())
        && !sex.equals(Sex.ETC.name())) {
      errors.add(new ValidationError("Sex must be \"MALE\" or \"FEMALE\" or \"ETC\""));
    }
    if (phoneNum == null || !pattern.matcher(phoneNum).matches()) {
      errors.add(new ValidationError(
          "Phone number must match pattern ^01([0|1|6|7|8|9])-\\d{3,4}-\\d{4}$."));
    }
    if (!errors.isEmpty()) {
      throw new ValidationErrors(errors);
    }
  }

  @Transactional
  public void update(UpdateRequest request) {

    User user = userRepository.findUser(request.userId());
    if (!LoggedInUserContext.get().getUserId().equals(user.getUserId().getValue())) {
      throw new AccessDeniedException("Cannot update other user's information.");
    }

    user.update(request.name(), request.age(), request.location(), request.phoneNum());
  }

  public void cancelUserRegistration(String userId) {
    userRepository.delete(userId);
  }
}
