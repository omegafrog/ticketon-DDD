# GET /static/events/images/{fileName}

- Controller: `StaticController.getEventImage()`
- Actor: 브라우저
- Goal: 정적 파일 또는 공용 엔드포인트를 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/app/src/main/java/org/codenbug/app/ui/StaticController.java`

## Use Case

정적 파일 또는 공용 엔드포인트를 처리한다.

## Success Criteria

- 요청은 `GET` `/static/events/images/{fileName}` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<Resource>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/app/StaticController/getEventImage.md)
- [Trouble](../../trouble/app/StaticController/getEventImage.md)
- [Troubleshooting](../../troubleshooting/app/StaticController/getEventImage.md)
