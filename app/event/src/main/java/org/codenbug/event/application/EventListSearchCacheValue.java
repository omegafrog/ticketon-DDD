package org.codenbug.event.application;

import org.codenbug.event.query.EventListProjection;

import java.util.List;

public record EventListSearchCacheValue(List<EventListProjection> eventListProjection, int total) {
}
