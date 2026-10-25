# 인천대학교 연속간행물실 근무 대시보드 — Backend

Java 21 + Spring Boot 3.5.16 + SQLite 기반 근무 관리 API 서버다. 이 저장소는 API·인증·DB·백업·서버 배포 코드만 관리한다.

React 화면·CSS·로고·글꼴·Vercel 설정은 [프론트엔드 저장소](https://github.com/eatyummydance200/library-dashboard-frontend)에서 관리한다. 이 서버는 HTML 로그인 페이지나 정적 화면을 제공하지 않는다. 이전 화면은 분리 전 Git 이력에서 확인할 수 있다.

## 구조

```text
src/main/java/dev/library/  API, JSON 인증, 세션, DB
src/main/resources/        서버 설정, DB 마이그레이션
src/test/                  Java 단위 테스트
tests/                     실제 HTTP 서버 기반 API 회귀 테스트
schema.sql, seed.json      초기 스키마와 빈 데이터 템플릿
scripts/                   로컬 실행, Server Desk 배포 보조 도구
ops/                       DB 백업용 systemd service/timer
Dockerfile                 백엔드 이미지
```

## 로컬 실행과 검증

Java 21, Maven, Python 3이 필요하다. 프론트 의존성 설치나 글꼴 다운로드는 필요 없다.

| 환경변수 | 용도 |
| --- | --- |
| `ADMIN_USER` | 첫 실행 때 생성할 관리자 아이디 |
| `ADMIN_PASSWORD` | 초기 비밀번호, 10자 이상. 저장소나 이미지에 넣지 않음 |
| `DATABASE` | DB 파일 경로, 기본 `/app/data/library.sqlite3` |
| `PORT` | 기본 `8000`, 프론트 개발 프록시 연결 시 `8090` |
| `APP_BASE_PATH` | 기본 `/library-dashboard` |
| `COOKIE_SECURE` | 기본 `true`, 로컬 HTTP 개발 때만 `false` |
| `INITIAL_SCHEDULE_JSON` | 선택 사항. 첫 실행 때 가져올 비공개 근무자·시간표 JSON |

`ADMIN_USER`, `ADMIN_PASSWORD`를 환경변수로 설정한 뒤 실행한다.

```bash
mvn -B verify
python3 -m unittest discover -s tests -v

export DATABASE="$PWD/data/local.sqlite3"
export PORT=8090
export COOKIE_SECURE=false
java -Xms32m -Xmx256m -jar target/app.jar
```

비공개 `.runtime.env`가 있으면 `python3 scripts/run-local.py`로 빌드·실행할 수도 있다. 기본 포트는 `8090`이며 `PORT`로 변경한다. 프론트는 별도 저장소에서 `npm run dev`로 실행한다.

`seed.json`은 빈 템플릿이다. 실제 근무자·연락처·비밀번호·DB·환경변수 파일은 커밋하지 않는다. 기존 DB는 초기 관리자·초기 데이터 환경변수로 덮어쓰지 않는다.

## API와 인증

모든 경로는 기본 `/library-dashboard` 아래에 있다.

| 경로 | 설명 |
| --- | --- |
| `GET /healthz` | 비로그인 상태 확인, `{"status":"ok"}` |
| `GET /api/auth/csrf` | 로그인 전 CSRF 토큰 발급, `{"csrf":"..."}` |
| `POST /api/auth/login` | JSON 로그인, 새 CSRF 토큰 반환 |
| `GET /api/me` | 로그인 계정과 CSRF 토큰 조회 |
| `POST /api/logout` | 세션 종료 |
| `/api/schedule`, `/api/shifts`, `/api/status` | 시간표·근무 구간·근무 현황 |
| `/api/staff`, `/api/teachers`, `/api/duty` | 근무자·선생님·당직 연락처 |
| `/api/issues`, `/api/tagging`, `/api/archive` | 이슈·태깅·과월호 |
| `/api/users`, `/api/password`, `/api/backup` | 계정·비밀번호·관리자 백업 |

로그인은 `{"username":"...","password":"..."}` JSON과 `X-CSRF-Token` 헤더를 보낸다. 로그인 성공 시 세션을 교체하므로 응답의 새 CSRF 토큰을 사용한다. 모든 쓰기 API에는 세션 쿠키와 CSRF 헤더가 필요하다. 비로그인 요청은 JSON `401`이며 HTML로 이동하지 않는다. `/login`, `/static/*`, 루트 업무 화면은 제거했다.

프론트의 Vite·Vercel 프록시가 API 요청을 같은 경로의 Java 서버에 전달한다. 브라우저는 프론트 출처의 세션 쿠키를 사용하므로 별도 CORS 허용은 필요 없다. 운영 API의 [상태 확인 주소](https://eatyummydance200.tail72d30b.ts.net/library-dashboard/healthz)를 유지한다.

로그인 시도 제한, 관리자·근무자·본인 담당 권한, 64KB JSON 요청 제한을 유지한다. 쿠키는 HttpOnly·SameSite=Lax이며 운영에서는 Secure를 사용한다. 비밀번호·계정 상태·근무자 연결 변경 시 기존 세션을 무효화한다. 서버 재시작 후에는 다시 로그인한다.

## 데이터 규칙

- 근무 구간은 정수 분의 시작 포함·종료 제외 구간이다. 09:00~22:00, 30분 단위이며 같은 사람의 겹친 근무는 거절한다.
- 오픈은 09:00 근무자, 마감은 날짜별 마지막 종료 시각(최소 21:00)에 종료한 근무자를 한 번씩 센다. 일반·데스크 역할은 별도 구간이다.
- 주 복사는 대상 주가 비어 있을 때만 허용한다. 비활성 근무자의 과거 기록은 보존한다. 이슈 조회는 최근 200건이다.
- 날짜·주간 경계·당직 초기화는 `Asia/Seoul`, 생성·수정 시각은 UTC 기준이다. 당직은 저장한 한국 날짜에만 유효하다.
- 태깅은 연도·월별로 저장하며 관리자만 변경한다. 과월호는 재직 근무자 7명에게 7개 묶음을 한 트랜잭션으로 추첨하거나 수동 배정한다. 근무자는 본인 담당 항목만 완료 처리한다.
- 과월호 담당자를 바꿔도 항목 ID·완료 상태·체크 계정·시각을 유지하며 새 담당자에게 권한을 넘긴다. 동시 배정 변경은 덮어쓰지 않는다.
- 선생님은 근로학생·시간표·태깅·과월호 인원과 분리한다. 당직은 연결된 선생님의 최신 연락처를 조회한다.
- 시작 시 버전별 SQL로 DB를 v7까지 업그레이드한다. 이번 저장소 분리는 스키마와 저장 데이터를 바꾸지 않는다. 기존 scrypt/PBKDF2-SHA256 비밀번호 해시도 유지한다.

## Server Desk 배포

PR에서는 Java 단위 테스트와 Python API 회귀 테스트를 실행한다. `main` push 또는 수동 실행에서는 검증 후 `ghcr.io/eatyummydance200/library-dashboard:main`과 커밋 SHA 태그를 게시한다. 기존 Server Desk와 호환되도록 이미지 이름을 유지한다. 운영 반영은 Server Desk에서 실행한다.

| 항목 | 값 |
| --- | --- |
| 앱 이름 | `library-dashboard` |
| 호스트 / 컨테이너 포트 | `8090` / `8000` |
| 바인딩 | `127.0.0.1` |
| 상태 확인 | `/library-dashboard/healthz` |
| 볼륨 | `data:/app/data` → `sd-library-dashboard-data` |
| 사용자 | UID `10001` |
| 자동 업데이트 | 초기 꺼짐 |

기존 Funnel의 `/library-dashboard` → `http://127.0.0.1:8090/library-dashboard` 연결과 데이터 볼륨을 유지한다. 프론트를 먼저 연결·검증한 뒤 API 전용 백엔드를 배포한다. 사용자는 별도 프론트 주소로 접속한다.

`python3 scripts/deploy.py bootstrap`은 초기 로컬 이미지 기동, `python3 scripts/deploy.py deploy`는 Server Desk 이미지 배포를 수행한다. 비공개 `.runtime.env`와 관리 서비스의 `.env`를 사용하며 관리 서비스 파일 위치는 `--server-desk-env`로 지정한다.

운영 JVM은 힙 최대 256MB, 메타스페이스 최대 128MB다. 한 서버·한 프로세스·소규모 팀 기준이며 SQLite DB를 여러 컨테이너에서 공유하지 않는다.

## 백업과 복구

관리자는 `GET /api/backup`으로 DB를 다운로드한다. 다음 명령은 SQLite `VACUUM INTO`로 일관된 백업을 만들고 7일 이전 백업을 정리한다.

```bash
docker exec sd-library-dashboard java -jar /app/app.jar --backup
```

`ops/`의 타이머는 매일 한국 시간 04시에 실행한다. 백업은 별도 기기에도 보관한다. 복구 시 서비스를 중지하고 데이터 디렉터리 전체를 보관한 뒤 `PRAGMA integrity_check`를 통과한 백업을 복원한다. 이전 `-wal`, `-shm` 파일을 섞지 않고 UID 10001을 유지한다. 이미지 롤백은 DB를 되돌리지 않으므로 호환되는 백업이 필요하다.

설계와 저장소 경계는 [ARCHITECTURE.md](ARCHITECTURE.md)를 참고한다.

## 커밋 컨벤션

커밋 제목은 `이모지 태그 : 변경 내용` 형식이다. 태그의 철자·대소문자와 콜론 앞뒤 공백을 유지하며 한 커밋에는 하나의 목적을 담는다.

| 태그 | 설명 |
| --- | --- |
| ⚙️ Setting : | 개발 환경 세팅 |
| ✅ Test : | test 관련 |
| ✨ Feature : | 기능 개발 |
| 🌍 Ci : | 배포 관련 |
| 🪲 Fix : | 버그 수정 |
| 📄 Docs : | 문서 작성 및 수정 |
| 🔧 Chore : | 자잘한 수정 |
| 🔨 Refactor : | 코드 리팩토링 |

```text
⚙️ Setting : 로컬 SQLite 실행 환경 설정
✅ Test : 로그인 CSRF 검증 테스트 추가
✨ Feature : 근무 시간표 조회 API 추가
🌍 Ci : 백엔드 이미지 빌드 워크플로 수정
🪲 Fix : 당직 연락처 날짜 초기화 오류 수정
📄 Docs : API 실행 방법 문서화
🔧 Chore : 사용하지 않는 설정 정리
🔨 Refactor : 인증 처리 로직 분리
```

본문이 필요하면 제목 다음 한 줄을 비우고 변경 이유·영향을 적는다. PR을 squash merge할 때도 같은 제목 형식을 사용한다.
