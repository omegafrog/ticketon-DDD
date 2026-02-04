# Serena MCP (Serena) 사용하기

이 리포지토리에서 Serena MCP 서버를 바로 붙여서(IDE/에이전트에서) 코드 탐색/편집 도구로 사용할 수 있게 설정했습니다.

## 1) 준비물

- `uv` / `uvx` (Astral uv)

설치(권장):

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

## 2) 프로젝트 설정 파일

- VS Code / Cursor: `.vscode/mcp.json`

현재 설정은 `uvx`로 Serena를 Git URL에서 바로 실행합니다.

IDE에서 서버를 띄울 때 초기 실행(의존성 설치/빌드) 때문에 타임아웃이 날 수 있어서, `.vscode/mcp.json`에 `--enable-web-dashboard false`를 추가해 시작 시간을 줄였습니다.

## 3) 빠른 시작

```bash
./scripts/setup-serena.sh
```

그 다음, MCP를 지원하는 클라이언트(예: Cursor/VS Code MCP 확장)가 `.vscode/mcp.json`을 읽도록 설정하면 됩니다.

## 4) (선택) 인덱싱

처음 1회 인덱싱을 해두면 탐색 성능이 좋아질 수 있습니다.

```bash
uvx --from git+https://github.com/oraios/serena serena project create --index "$(pwd)"
```

## 4-1) 타임아웃이 나는 경우(가장 흔한 원인)

- IDE가 `uvx`를 못 찾거나(PATH), 처음 실행 시 `uvx --from ...`가 패키지 설치/빌드로 오래 걸리면 MCP가 타임아웃으로 보일 수 있습니다.
- 해결:
  - `./scripts/setup-serena.sh`를 한 번 실행해서 `uvx` 설치/캐시 워밍업을 끝낸 뒤, IDE를 재시작하세요.
  - Windows에서 VS Code/Cursor를 Windows에서 실행 중이면(WSL이 아니라), Windows 쪽 PATH에 `uvx`가 잡혀 있어야 합니다.

## 5) Claude Desktop / Claude Code 사용 시

Serena 리포지토리 README 기준으로는 클라이언트마다 설정 키가 다릅니다.

- Claude Desktop 예시(개인 설정 파일에 추가)

```json
{
  "mcpServers": {
    "serena": {
      "command": "uvx",
      "args": [
        "--from",
        "git+https://github.com/oraios/serena",
        "serena",
        "start-mcp-server",
        "--context",
        "desktop-app"
      ]
    }
  }
}
```

## 6) OpenCode(opencode)에서 사용 시

OpenCode는 전역 설정(`~/.config/opencode/opencode.json`)의 `mcp` 섹션에 MCP 서버를 등록해서 사용합니다.

예시(Serena, 로컬 stdio):

```json
{
  "mcp": {
    "serena": {
      "type": "local",
      "enabled": true,
      "command": [
        "uvx",
        "--from",
        "git+https://github.com/oraios/serena",
        "serena",
        "start-mcp-server",
        "--context",
        "ide",
        "--project",
        "{env:PWD}",
        "--enable-web-dashboard",
        "false"
      ]
    }
  }
}
```

확인:

```bash
opencode mcp list
```

## 7) 참고

- Serena: https://github.com/oraios/serena
