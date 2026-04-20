# Docs Workflow

이 저장소의 문서는 엔드포인트별로 분리된 다음 4개 축으로 읽는다.

- `usecase/`: 액터 관점의 요구사항
- `flow/`: 구현 흐름
- `trouble/`: 이전 구현과 문제 인식
- `troubleshooting/`: 해결 방식과 검증 수치

읽는 순서는 보통 `usecase -> flow -> trouble -> troubleshooting`이다.
모든 문서는 `docs/<stage>/<module>/<controller>/<method>.md` 형태로 분리되어 있다.

## Entry Points

- [Use Cases](usecase/README.md)
- [Flow](flow/README.md)
- [Trouble](trouble/README.md)
- [Troubleshooting](troubleshooting/README.md)
