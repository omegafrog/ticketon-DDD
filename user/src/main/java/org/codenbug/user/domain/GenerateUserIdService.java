package org.codenbug.user.domain;

import com.fasterxml.uuid.Generators;

public class GenerateUserIdService {
	UserId generateUserId() {
		return new UserId(Generators.timeBasedEpochGenerator().generate().toString());
	}
}
