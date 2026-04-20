# GET /api/v1/events/{event-id}/seats Troubleshooting

## Current State

- seat select/cancel은 queue token과 같이 검증된다.

## Verification

- double booking이 발생하지 않는지 확인한다.
- cancel이 slot release와 함께 처리되는지 본다.

## Quantitative Notes

- race window count: `1` critical write path

## Recent History

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)



## Related Docs

- [Use Case](../../usecase/seat/SeatQueryController/getSeatLayout.md)
- [Flow](../../flow/seat/SeatQueryController/getSeatLayout.md)
- [Trouble](../../trouble/seat/SeatQueryController/getSeatLayout.md)
