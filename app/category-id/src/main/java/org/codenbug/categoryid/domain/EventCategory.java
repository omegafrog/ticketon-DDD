package org.codenbug.categoryid.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventCategory {

    @EmbeddedId
    private CategoryId id;
    private String name;
    private String thumbnailUrl;
}
