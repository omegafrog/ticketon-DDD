# GET /api/v1/categories

- Controller: `EventCategoryController.getAllCategories()`
- Actor: 외부 호출자
- Goal: 이벤트 조회/변경/내부 검증/이미지/배치를 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/event/src/main/java/org/codenbug/event/category/ui/EventCategoryController.java`

## Use Case

이벤트 조회/변경/내부 검증/이미지/배치를 처리한다.

## Success Criteria

- 요청은 `GET` `/api/v1/categories` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<List<EventCategoryListResponse>>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/event/EventCategoryController/getAllCategories.md)
- [Trouble](../../trouble/event/EventCategoryController/getAllCategories.md)
- [Troubleshooting](../../troubleshooting/event/EventCategoryController/getAllCategories.md)
