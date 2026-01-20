package org.codenbug.event.category.infra;

import org.codenbug.event.category.domain.CategoryId;
import org.codenbug.event.category.domain.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEventCategoryRepository extends JpaRepository<EventCategory, CategoryId> {

}
