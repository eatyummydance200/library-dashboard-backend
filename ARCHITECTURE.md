# 근무 대시보드 아키텍처

## 저장소와 실행 경계

| 구성 | 저장소 | 실행 위치 |
| --- | --- | --- |
| React 19 · TypeScript · Vite, CSS·로고·글꼴 | [library-dashboard-frontend](https://github.com/eatyummydance200/library-dashboard-frontend) | Vercel 또는 Vite 개발 서버 |
| Java 21 · Spring Boot · 인증 · API · DB | [library-dashboard-backend](https://github.com/eatyummydance200/library-dashboard-backend) | 기존 Server Desk의 Docker 컨테이너 |

브라우저 → 프론트 서버 → `/library-dashboard/api/*` 프록시 → Java API → SQLite 순서로 요청한다. 프론트는 백엔드 파일을 직접 읽거나 빌드에 포함하지 않는다. 백엔드에는 HTML 템플릿·정적 화면·Node.js 빌드 단계가 없다. 이전 서버 렌더링 화면과 전용 테스트는 분리 전 Git 이력에 남는다.

## API와 세션

Vite는 개발 요청을 `http://127.0.0.1:8090`으로, Vercel은 운영 요청을 기존 Funnel의 동일 경로로 전달한다. `/library-dashboard` 접두 경로와 API 응답 형식은 유지한다.

`GET /api/auth/csrf` → `POST /api/auth/login` → `GET /api/me` 순서로 로그인한다. 쓰기는 `X-CSRF-Token`을 검증하며 로그인 시 세션과 CSRF 토큰을 교체한다. 미인증 요청은 JSON `401`, 권한·CSRF 오류는 JSON `403`이다. 로그인 시도 제한과 비밀번호 해시 동시 계산 제한을 유지한다.

쿠키는 `library_dashboard_session`, Path는 `/library-dashboard`, 속성은 HttpOnly·SameSite=Lax·운영 Secure다. 브라우저는 프론트 출처의 쿠키를 사용하며 직접 교차 출처 요청을 보내지 않는다. 프론트와 Java 서버 도메인의 세션은 별개다. API 응답은 캐시하지 않는다. 개인정보·비밀번호·DB 파일은 프론트 번들에 포함하지 않는다.

## 데이터와 권한

SQLite 파일은 `/app/data/library.sqlite3`에 둔다. 외래키·WAL·연결별 busy timeout과 짧은 쓰기 트랜잭션을 유지하며 컨테이너는 하나만 실행한다.

관리자만 시간표·인적정보·계정·태깅·배정을 변경한다. 근무자는 이슈를 작성하고 본인 담당 과월호를 완료 처리한다. 계정 비활성화·비밀번호·근무자 연결 변경은 기존 세션을 무효화한다.

근무 구간은 정수 분의 시작 포함·종료 제외 구간이다. 날짜·당직 초기화는 `Asia/Seoul`, 기록 시각은 UTC다. 근무자 비활성화 시 과거 기록을 보존한다. 시작 시 버전별 SQL로 DB v7까지 마이그레이션하며 이번 분리로 스키마를 변경하지 않는다.

## 배포와 검증

백엔드 CI는 Java 단위 테스트와 실제 HTTP 서버 기반 Python API 회귀 테스트를 실행한다. 검증 후 `main`의 이미지를 기존 GHCR 이름으로 게시한다. 프론트 CI는 자체 레포에서 `npm ci`, `npm run build`를 실행한다.

백엔드는 UID 10001, 포트 8000, 호스트 바인딩 `127.0.0.1:8090`을 유지한다. Funnel 대상은 `http://127.0.0.1:8090/library-dashboard`, 상태 확인은 `/library-dashboard/healthz`다. 프론트를 먼저 연결한 뒤 API 전용 백엔드를 배포한다. 기존 루트 Server Desk 연결·영구 볼륨·DB 내용은 유지한다.

일일 백업은 SQLite `VACUUM INTO`로 생성하고 최근 7일을 보관한다. 별도 기기에도 복사한다. 실행·복구·DB 호환성 절차는 [README.md](README.md)를 따른다.
