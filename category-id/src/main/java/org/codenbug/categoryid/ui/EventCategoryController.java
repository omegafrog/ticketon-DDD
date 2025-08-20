package org.codenbug.categoryid.ui;

import java.util.List;

import org.codenbug.categoryid.app.EventCategoryService;
import org.codenbug.categoryid.global.EventCategoryListResponse;
import org.codenbug.common.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class EventCategoryController {
    
    private final EventCategoryService eventCategoryService;

    public EventCategoryController(EventCategoryService eventCategoryService) {
        this.eventCategoryService = eventCategoryService;
    }

    @GetMapping
    public ResponseEntity<RsData<List<EventCategoryListResponse>>> getAllCategories() {
        List<EventCategoryListResponse> categories = eventCategoryService.getAllCategories();
        return ResponseEntity.ok(new RsData("200", "카테고리 리스트 조회 성공.", categories));
    }
}