package org.codenbug.user.domain;

import org.springframework.stereotype.Service;

import com.fasterxml.uuid.Generators;

@Service
public class GenerateUserIdService {
	UserId generateUserId() {
		return new UserId(Generators.timeBasedEpochGenerator().generate().toString());
	}
}
