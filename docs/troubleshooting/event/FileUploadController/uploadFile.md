# PUT /static/events/images/{fileName} Troubleshooting

## Current State

- internal validation은 purchase worker의 gate로만 쓰인다.
- query/update 경계가 섞이지 않았는지 확인한다.

## Verification

- boolean guard와 400 parse failure를 분리한다.
- 권한이 필요한 경로는 role guard가 적용되는지 확인한다.

## Quantitative Notes

- internal guard count: `1`
- failure mode count: `2` (`400`, `false`)

## Recent History

- [controller] `3fa11c3` (2026-01-20): delete: 불필요한 파일 및 설정 제거



## Related Docs

- [Use Case](../../usecase/event/FileUploadController/uploadFile.md)
- [Flow](../../flow/event/FileUploadController/uploadFile.md)
- [Trouble](../../trouble/event/FileUploadController/uploadFile.md)
