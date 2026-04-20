# GET /internal/seat-layouts/{layout-id} Troubleshooting

## Current State

- seat select/cancel은 queue token과 같이 검증된다.

## Verification

- double booking이 발생하지 않는지 확인한다.
- cancel이 slot release와 함께 처리되는지 본다.

## Quantitative Notes

- race window count: `1` critical write path

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `3fa11c3` (2026-01-20): delete: 불필요한 파일 및 설정 제거



## Related Docs

- [Use Case](../../usecase/seat/SeatInternalController/getSeatLayout.md)
- [Flow](../../flow/seat/SeatInternalController/getSeatLayout.md)
- [Trouble](../../trouble/seat/SeatInternalController/getSeatLayout.md)
