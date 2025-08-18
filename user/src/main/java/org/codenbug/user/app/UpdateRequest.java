package org.codenbug.user.app;

import org.codenbug.user.domain.UserId;


public record UpdateRequest(UserId userId, String name, Integer age, String location, String phoneNum) {
}
