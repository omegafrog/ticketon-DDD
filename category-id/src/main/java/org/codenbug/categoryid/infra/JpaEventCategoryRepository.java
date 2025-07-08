package org.codenbug.categoryid.infra;

import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEventCategoryRepository extends JpaRepository<EventCategory, CategoryId> {

}
