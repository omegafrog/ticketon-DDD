## 2026-01-28 Task: init
- 

## 2026-01-28 Task: k6 async
- Important: barrier Redis writes (`sendCommand`) must be awaited; made `export default` and `connectSse` async to avoid dropping Promises.
