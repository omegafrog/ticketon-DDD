package org.codenbug.event.global.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CostRange {

    private Integer min = 0;
    private Integer max = 0;

    public CostRange(Integer min, Integer max) {
		if (min > max) {
			throw new IllegalArgumentException("min should be less than max");
		}
        this.min = min;
        this.max = max;

    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }
}
