package org.codenbug.purchase.domain;

import org.codenbug.purchase.query.model.EventProjection;

public interface EventProjectionRepository {
	boolean existById(String eventId);

	EventProjection findByEventId(String eventId);
}
