# 인천대학교 연속간행물실 근무 대시보드 — Backend

Java 21 · Spring Boot 4.1.2 · Gradle 9.7.1 · PostgreSQL 18 기반 API다.
프론트는 [별도 저장소](https://github.com/eatyummydance200/library-dashboard-frontend)에서 관리한다.

## 실행

Java 21이 필요하다. Gradle은 Wrapper가 내려받는다. 포트는 **8080**, 기본 경로는 `/library-dashboard`다.

```bash
cp .env.example .env
# .env의 DB_PASSWORD, ADMIN_USER, ADMIN_PASSWORD를 실제 개발용 값으로 변경
# Docker Compose는 .env를 자동으로 읽는다.
docker compose up --build -d
```

DB만 Docker로 띄우고 Java를 직접 실행할 수도 있다.

```bash
docker compose up -d postgres
# .env 값들을 셸 환경변수로 설정한 뒤 실행한다. bootRun은 .env를 자동으로 읽지 않는다.
./gradlew bootRun
# Windows PowerShell: .\gradlew.bat bootRun
```

| 환경변수 | 설명 |
| --- | --- |
| `DB_URL` | JDBC 주소, 기본 `jdbc:postgresql://localhost:5432/library_dashboard` |
| `DB_USER` | DB 계정, 기본 `library_dashboard` |
| `DB_PASSWORD` | 필수 DB 비밀번호 |
| `ADMIN_USER`, `ADMIN_PASSWORD` | 빈 DB의 최초 관리자. 아이디 영문·숫자 등 2~32자, 비밀번호 10~128자 |
| `APP_BASE_PATH` | 기본 `/library-dashboard`, `/` + 영문·숫자·밑줄·하이픈 |
| `COOKIE_SECURE` | 기본 `true`. 로컬 HTTP에서만 `false` |

기존 사용자가 있으면 초기 관리자 환경변수로 덮어쓰지 않는다. 초기 시간표 JSON 자동 주입은 제거했다. 새 DB의 근무자는 API에서 등록하거나 아래 이관 도구로 가져온다.

```bash
./gradlew build
java -jar build/libs/app.jar
```

요청에 따라 테스트 소스와 테스트 의존성은 모두 제거했다. `build`와 CI는 컴파일·패키징을 확인한다.

## API와 데이터

기존 URL과 JSON 응답을 유지한다. `/api/backup`의 파일 형식만 SQLite에서 JSON으로 변경했다.

| 경로 | 설명 |
| --- | --- |
| `GET /healthz` | DB 연결을 포함한 기존 상태 확인 |
| `GET /api/auth/csrf` | 로그인 전 CSRF 토큰 |
| `POST /api/auth/login` | JSON 로그인, 새 CSRF 토큰 반환 |
| `GET /api/me`, `POST /api/logout` | 로그인 계정·로그아웃 |
| `/api/schedule`, `/api/shifts`, `/api/status` | 시간표·근무 구간·현황 |
| `/api/staff`, `/api/teachers`, `/api/duty` | 근무자·선생님·당직 |
| `/api/issues`, `/api/tagging`, `/api/archive` | 이슈·태깅·과월호 |
| `/api/users`, `/api/password` | 관리자 계정 관리·비밀번호 변경 |
| `GET /api/backup` | 관리자 전용 업무 데이터 JSON 다운로드. 비밀번호 해시와 연락처 포함 |

위 경로 앞에는 `/library-dashboard`를 붙인다. 로그인 본문은 `{"username":"...","password":"..."}`다. 모든 쓰기는 세션 쿠키와 `X-CSRF-Token`을 사용한다. 로그인 후 새 토큰을 사용한다. 미인증은 JSON 401, 권한·CSRF 오류는 403, 충돌은 409다. 요청 본문은 최대 64KB다.

세션은 PostgreSQL에 저장하며 여러 Pod 및 재시작 사이에 공유한다. 유효 기간은 12시간이다. 비밀번호·계정 상태·근무자 연결 변경 시 기존 세션이 무효화된다. 쿠키 이름은 `library_dashboard_session`, HttpOnly·SameSite=Lax·운영 Secure다. 프론트 프록시는 쿠키와 CSRF 헤더를 전달해야 한다. 별도 CORS 허용은 없다.

프론트 개발 프록시의 대상 포트를 `http://127.0.0.1:8080`으로 바꾼다. 운영 프록시는 실제 Ingress 주소로 변경한다. 프론트 저장소는 이번 백엔드 변경에 포함하지 않는다.

- 업무 날짜·당직 초기화는 `Asia/Seoul`, 기록 시각은 UTC다.
- 근무는 평일 09:00~22:00, 30분 단위다. 같은 사람의 겹친 근무와 이미 시간표가 있는 주로 복사는 거절한다.
- 업무상의 `desk` 근무 역할은 유지한다. 제거 대상은 앱 배포 관리 기능이다.
- 과월호는 재직 근무자 7명에게 배정한다. 담당 변경 시 항목 ID·완료 상태·완료자·시각을 보존한다. 근무자는 본인 담당만 완료 처리한다.
- Flyway가 `src/main/resources/db/migration`의 버전별 SQL을 적용한다. 적용된 SQL은 수정하지 않고 새 버전을 추가한다.
- 작은 팀을 기준으로 업무 쓰기를 PostgreSQL 트랜잭션 잠금 하나로 직렬화한다. Pod가 달라도 검사와 저장 사이에 다른 쓰기가 끼어들지 않는다. 읽기는 일관된 DB 스냅샷을 사용한다. 쓰기 대기가 커지면 업무별 잠금으로 분리한다.

## Kubernetes

외부 또는 클러스터에서 관리하는 PostgreSQL을 사용한다. API Pod에는 DB 볼륨이나 Docker 소켓이 필요 없다.

1. 운영 환경변수 파일을 저장소 밖에 만든다. `DB_URL`, `DB_USER`, `DB_PASSWORD`, 최초 관리자를 넣고 파일 권한을 제한한다.
2. 같은 namespace에 Secret을 생성한다.
3. CI가 만든 커밋 SHA 이미지로 배포한다.

```bash
kubectl create secret generic library-dashboard --from-env-file=/private/library-dashboard.env
# k8s/deployment.yaml의 image 태그를 배포할 커밋 SHA로 변경
kubectl apply -f k8s/deployment.yaml
kubectl rollout status deployment/library-dashboard
```

예시는 Deployment 2 replicas + ClusterIP Service 8080이다. 읽기 전용 루트 파일시스템, UID 10001, 임시 `/tmp` 볼륨, 리소스 제한을 포함한다. 30초 종료 유예 중 Spring은 최대 20초간 진행 중 요청을 마무리한다.

| Probe | 경로 | 의미 |
| --- | --- | --- |
| startup / liveness | `/library-dashboard/actuator/health/liveness` | JVM·앱 생존, DB 장애로 재시작하지 않음 |
| readiness | `/library-dashboard/actuator/health/readiness` | DB를 사용할 수 있어야 요청 전달 |

`APP_BASE_PATH`를 바꾸면 probe 경로도 바꾼다. Pod당 DB 풀은 최대 5개다. Pod 수와 PostgreSQL 연결 한도를 함께 잡는다. HTTPS Ingress의 호스트·인증서·DB 주소는 클러스터에 맞게 설정한다. Ingress는 기본 경로를 제거하지 않고 Service 8080으로 전달한다. 첫 배포 전 Secret과 이미지를 준비해야 한다.

## 기존 데이터 이관 / JSON 복원

운영 DB를 자동으로 변환하거나 삭제하지 않는다. 이관 중에는 구·신 앱의 쓰기를 모두 중지한다.

1. 구 앱이 정상 종료된 SQLite **v7** 스냅샷을 별도로 보관한다. 실행 중인 DB 파일 하나만 복사하면 WAL의 데이터가 빠질 수 있다. 구 앱의 백업 파일을 쓰거나 서비스를 중지하고 복사한다.
2. 새 PostgreSQL DB에서 앱을 한 번 실행해 Flyway 스키마와 초기 관리자 한 명을 만든 뒤 앱을 중지한다.
3. 별도 Python 환경에 이관 도구 의존성을 설치한다. 아래 `PG*` 환경변수를 대상 DB에 맞게 설정한다.

```bash
python -m pip install 'psycopg[binary]>=3.2,<4'
export PGHOST=localhost PGPORT=5432 PGDATABASE=library_dashboard PGUSER=library_dashboard
# PGPASSWORD는 환경변수 또는 .pgpass로 안전하게 설정
python scripts/import-data.py /private/library-v7.sqlite3 --replace-bootstrap-admin
# 새 JSON 내보내기도 같은 명령으로 복원
python scripts/import-data.py /private/library-export.json --replace-bootstrap-admin
```

둘 중 필요한 명령 하나만 실행한다. 대상은 업무 데이터가 없고 관리자 한 명만 있는 DB여야 한다. 옵션은 그 초기 관리자를 원본 계정들로 교체한다는 뜻이다. 하나라도 실패하면 데이터 변경은 롤백한다. ID·비밀번호 해시·기존 담당 기록을 유지하고 자동 증가 ID를 맞춘다. 이전 세션은 옮기지 않아 다시 로그인해야 한다. SQLite 원본은 읽기 전용으로 연다. 이관 후 인원·시간표·배정·완료 기록을 비교하고 앱을 시작한다.

## 운영 백업

API JSON은 업무 데이터 이관용이며 전체 PostgreSQL 백업을 대체하지 않는다. 운영에서는 DB 운영자/관리형 서비스의 정기 백업과 복구 정책을 사용한다. 예를 들어 PostgreSQL 18 클라이언트와 `PG*` 환경변수로 실행한다.

```bash
pg_dump --format=custom --file=/private/library.dump
# 빈 복구 DB를 PGDATABASE로 지정한 뒤 실행
pg_restore --exit-on-error --single-transaction --no-owner --dbname="$PGDATABASE" /private/library.dump
```

덤프·JSON에는 민감 정보가 있으므로 암호화된 별도 저장소에 보관한다. 복구·이관은 원본과 분리한 빈 DB에서 먼저 확인한다. 이미지 롤백은 DB 스키마를 되돌리지 않는다.

## CI와 구조

PR은 Gradle 빌드를 수행한다. main push/수동 실행은 빌드 후 GHCR에 `main` 및 커밋 SHA 이미지를 게시한다. 클러스터 반영은 배포 관리자가 수행한다.

```text
src/main/java/dev/library/        API · 인증 · JDBC
src/main/resources/db/migration/  PostgreSQL / 세션 스키마
scripts/import-data.py            SQLite v7 / JSON → 새 PostgreSQL
k8s/deployment.yaml               Deployment · Service
compose.yaml                     로컬 PostgreSQL · API
AUDIT.md                         개편 전 리뷰와 조치 결과
```

커밋 제목은 기존 `이모지 태그 : 변경 내용` 형식을 따른다. 설정 `⚙️ Setting :`, 기능 `✨ Feature :`, 배포 `🌍 Ci :`, 수정 `🪲 Fix :`, 문서 `📄 Docs :`, 정리 `🔧 Chore :`, 개편 `🔨 Refactor :`를 쓴다.

## 원본 근무표와 주당 근무 시간

`V2__original_schedule.sql`이 합의한 원본과 주당 시간 조정값을 별도로 저장한다. 앱 재시작 시 Flyway가 적용한다. 실제 `shifts`의 수정·삭제·주 복사는 원본이나 조정값을 변경하지 않는다. 신규 테이블은 JSON 백업에도 포함한다.

| 경로 (`/library-dashboard/api` 기준) | 동작 |
| --- | --- |
| `GET /schedule/original?week=YYYY-MM-DD` | 원본 조회. 미저장 주는 `saved: false`, `shifts: []` |
| `PUT /schedule/original` | `{week, shifts}`로 원본 저장·교체. 실제 근무 ID는 재사용하지 않음 |
| `GET /schedule/hours?week=YYYY-MM-DD` | `{overrides: [...]}`로 수동 조정값 조회 |
| `PUT /schedule/hours/{staff_id}` | `{week, total_minutes, desk_minutes, opens, closes}` 저장 |
| `DELETE /schedule/hours/{staff_id}?week=YYYY-MM-DD` | 조정값만 삭제해 원본 자동 계산으로 복원 |

조회는 로그인, 저장·복원은 관리자 권한과 CSRF 토큰이 필요하다. 주는 월요일로 정규화한다. 원본은 해당 주의 평일·09:00~22:00·30분 단위·동일인 시간 겹침을 검증하고 트랜잭션으로 교체한다. 시간 조정은 0~3900분의 30분 단위, 데스크 시간은 총 시간 이하, 오픈·마감은 각각 0~5회다. 원본 재저장 시 수동 조정값은 유지된다. 기존 실제 근무표를 자동으로 원본으로 간주하지 않으며 관리자가 최초 원본을 저장한다.
