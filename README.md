# 인천대학교 연속간행물실 근무 대시보드 — Backend

Java 21 · Spring Boot 4.1.2 · Spring Data JPA · Gradle 9.7.1 기반 API다. 로컬은 파일 기반 H2, 운영은 PostgreSQL 18을 사용한다.
프론트는 [별도 저장소](https://github.com/eatyummydance200/library-dashboard-frontend)에서 관리한다.

## 실행

Java 21이 필요하다. Gradle은 Wrapper가 내려받는다. 포트는 **8080**, 기본 경로는 `/library-dashboard`다.

로컬에서는 별도 DB 서버 없이 실행한다.

```powershell
.\gradlew.bat bootRun
```

IntelliJ에서는 별도 환경변수 없이 실행한다. 프로필을 지정하지 않으면 `local`로 시작한다. macOS/Linux에서는 `./gradlew bootRun`으로 실행한다. 계정은 자동 생성하지 않으므로 새 DB에서 로그인하려면 최초 관리자를 직접 등록해야 한다.

H2는 실행 디렉터리의 `data/library-dashboard.mv.db`에 업무 데이터와 세션을 저장한다. 앱을 재시작해도 유지되며 `data/`는 Git과 Docker 빌드에서 제외된다. 같은 DB 파일은 앱 한 개에서만 연다. 로컬에서는 `DB_URL`, `DB_USER`, `DB_PASSWORD`를 사용하지 않는다.

| 설정 파일 | 역할 |
| --- | --- |
| `application.yml` | 경로, 세션, DB 풀 등 공통 설정 |
| `application-local.yml` | 파일 H2, HTTP용 쿠키 |
| `application-prod.yml` | 필수 PostgreSQL 환경변수, Secure 쿠키, SQL 실행 제한 |

기본 프로필은 `local`이다. 운영에서는 반드시 `SPRING_PROFILES_ACTIVE=prod`를 지정한다. 잘못된 프로필에서 임시 메모리 DB가 자동 생성되지 않도록 내장 DB 자동 선택은 꺼 두었다.

로컬은 IntelliJ 또는 `bootRun`으로 파일 H2를 사용하고, 운영은 Kubernetes에서 PostgreSQL을 사용한다.
컨테이너 이미지용 `Dockerfile`은 유지하며 로컬 Docker Compose 구성은 제공하지 않는다.
IntelliJ와 `bootRun`은 `.env`를 자동으로 읽지 않는다. 환경변수는 실행 구성이나 셸에서 설정한다.

| 환경변수 | 설명 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` 또는 `prod`. 기본 local, Kubernetes는 prod로 지정 |
| `DB_URL` | 운영 필수 PostgreSQL JDBC 주소 |
| `DB_USER` | 운영 필수 DB 계정 |
| `DB_PASSWORD` | 운영 필수 DB 비밀번호 |
| `APP_BASE_PATH` | 기본 `/library-dashboard`, `/` + 영문·숫자·밑줄·하이픈 |
| `MANUAL_SEED_DIR` | `library-dashboard.manual.seed-dir`. 지정하고 매뉴얼 행이 없을 때만 시작 시 `<dir>/manual.md`와 `<dir>/images/*`를 DB에 넣는다. 매뉴얼 본문·이미지는 공개 레포에 넣지 않는다 |

최초 관리자 등록 이후 계정과 근무자는 해당 API에서 등록한다. 계정 생성 API에는 관리자 권한이 필요하다.

```bash
./gradlew build
java -jar build/libs/app.jar
```

Python 검증 스크립트와 별도 테스트 프레임워크는 사용하지 않는다. 현재 테스트 소스가 없으므로
Gradle 빌드는 컴파일·패키징을 확인하며 API 동작을 자동 검증하지 않는다.

## API와 데이터

업무 API는 JSON으로 요청·응답한다. 업무 데이터 내보내기·복원 API와 데이터 이관 스크립트는 제공하지 않는다.

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
| `GET`·`PUT /api/manual` | 근로학생 매뉴얼 `{content, updated_at, updated_by}` 조회(로그인)·저장(관리자, 최대 200,000자) |
| `POST /api/manual/images`, `GET /api/manual/images/{name}` | 매뉴얼 이미지 업로드(관리자, multipart `file`, PNG·JPEG·GIF·WEBP 5MB 이하, `{name}` 반환)·조회(로그인) |

위 경로 앞에는 `/library-dashboard`를 붙인다. 로그인 본문은 `{"username":"...","password":"..."}`다. 모든 쓰기는 세션 쿠키와 `X-CSRF-Token`을 사용한다. 로그인 후 새 토큰을 사용한다. 미인증은 JSON 401, 권한·CSRF 오류는 403, 충돌은 409다. 요청 본문은 최대 64KB다(매뉴얼 저장 2MB, 이미지 업로드 6MB 예외).

세션은 운영 PostgreSQL에 저장하며 여러 Pod 및 재시작 사이에 공유한다. 로컬 세션은 H2 파일에 저장해 재시작 후에도 유지한다. 유효 기간은 12시간이다. 비밀번호·계정 상태·근무자 연결 변경 시 기존 세션이 무효화된다. 쿠키 이름은 `library_dashboard_session`, HttpOnly·SameSite=Lax·운영 Secure다. 프론트 프록시는 쿠키와 CSRF 헤더를 전달해야 한다. 별도 CORS 허용은 없다.

프론트 개발 프록시의 대상 포트를 `http://127.0.0.1:8080`으로 바꾼다. 운영 프록시는 실제 Ingress 주소로 변경한다. 프론트 저장소는 이번 백엔드 변경에 포함하지 않는다.

- 업무 날짜·당직 초기화는 `Asia/Seoul`, 기록 시각은 UTC다.
- 근무는 평일 09:00~22:00, 30분 단위다. 같은 사람의 겹친 근무와 이미 시간표가 있는 주로 복사는 거절한다.
- 업무상의 `desk` 근무 역할은 유지한다. 제거 대상은 앱 배포 관리 기능이다.
- 과월호는 재직 근무자 7명에게 배정한다. 담당 변경 시 항목 ID·완료 상태·완료자·시각을 보존한다. 근무자는 본인 담당만 완료 처리한다.
- Flyway가 `src/main/resources/db/migration`의 버전별 SQL을 적용한다. 적용된 SQL은 수정하지 않고 새 버전을 추가한다.
- Service의 `@Transactional`이 업무 단위 트랜잭션을 관리한다. 조회는 read-only·repeatable read, 쓰기는 read committed다. 작은 팀을 기준으로 PostgreSQL advisory transaction lock이 Pod 사이의 쓰기를 보호한다. 파일 H2의 쓰기 잠금도 메서드 반환 시점이 아니라 커밋·롤백까지 유지한다.

## Kubernetes

외부 또는 클러스터에서 관리하는 PostgreSQL을 사용한다. Deployment는 `SPRING_PROFILES_ACTIVE=prod`를 지정한다. API Pod에는 DB 볼륨이나 Docker 소켓이 필요 없다. 운영 DB 주소·계정·비밀번호에는 기본값이 없어 누락 시 시작하지 않는다. 기존 `COOKIE_SECURE` 환경변수는 사용하지 않으며 prod 쿠키는 Secure다.

운영은 홈서버 단일 노드 클러스터의 `library-dashboard` namespace이며 Argo CD가 `k8s/`를 자동 동기화한다.
main에 push하면 CI가 이미지를 GHCR에 게시하고 `k8s/deployment.yaml`의 image 태그를 커밋 SHA로 바꿔 커밋한다.
Argo CD가 이 변경을 감지해 반영한다. Application 정의는 `argocd/application.yaml`이다.

Secret `library-dashboard`(`DB_URL`, `DB_USER`, `DB_PASSWORD`)와 `app-metrics`(`token`)는 클러스터에서 직접 관리한다.

Deployment 2 replicas, ClusterIP Service 8080(호스트 프록시용 고정 IP `10.96.20.10`), 지표 Service 8081이다. 읽기 전용 루트 파일시스템, UID 10001, 임시 `/tmp` 볼륨, 리소스 제한을 포함한다. 30초 종료 유예 중 Spring은 최대 20초간 진행 중 요청을 마무리한다.

스키마 변경으로 구 JDBC 버전과 새 JPA 버전을 동시에 실행할 수 없으므로 Deployment 전략은
`Recreate`다. 적용 전에 DB 스냅샷을 확보하고 해당 DB를 사용하는 구 버전 쓰기 프로세스를 모두
중지한다. 기존 Pod가 종료된 뒤 새 Pod가 시작하므로 배포 중 서비스 중단이 발생한다.
V3 적용 후 구 이미지로만 되돌리는 방식은 지원하지 않는다.

| Probe | 경로 | 의미 |
| --- | --- | --- |
| startup / liveness | 관리 포트 8081 `/actuator/health/liveness` | JVM·앱 생존, DB 장애로 재시작하지 않음 |
| readiness | 관리 포트 8081 `/actuator/health/readiness` | DB를 사용할 수 있어야 요청 전달 |

Pod당 DB 풀은 최대 5개다. 외부 노출은 Ingress 없이 호스트 프록시(127.0.0.1:18080 → 10.96.20.10:8080)와 Tailscale Funnel을 쓴다. `/actuator/prometheus`는 8081에서 `MONITORING_SCRAPE_TOKEN` Bearer 인증이 필요하다.

## CI와 구조

PR은 Gradle 빌드를 수행한다. main push/수동 실행은 빌드 성공 후
Dockerfile로 이미지를 빌드해 GHCR에 `main` 및 커밋 SHA 태그로 게시한다.
이어서 `k8s/deployment.yaml`의 image 태그를 갱신해 커밋하고, 클러스터 반영은 Argo CD가 수행한다.

```text
src/main/java/dev/library/        기능별 Controller · Service · Repository
src/main/resources/application*.yml 공통 / local H2 / prod PostgreSQL 설정
src/main/resources/db/migration/  공통 업무 / 세션 스키마
k8s/deployment.yaml               Deployment · Service
Dockerfile                       Kubernetes 실행용 컨테이너 이미지
AUDIT.md                         개편 전 리뷰와 조치 결과
```

## 코드 구조

`auth`, `user`, `staff`, `teacher`, `schedule`, `duty`, `issue`, `tagging`, `archive`,
`health` 패키지 안에서 계층을 나눈다.

| 계층 | 책임 |
| --- | --- |
| `*Controller` | HTTP 요청 수신, 상태 코드·응답 구성, 세션 생성·폐기 |
| `*Service` | 업무 규칙·권한 검사, 읽기·쓰기 트랜잭션 범위 |
| `*Repository` | Spring Data JPA 기본 CRUD·조회 메서드·JPQL·Specifications |
| `Entity` | Long PK·연관관계와 DB 테이블 매핑 |
| 응답 DTO | Entity를 직접 노출하지 않고 기존 JSON 필드·숫자 플래그를 유지 |
| `security` | 세션 인증, 로그인 사용자, CSRF·요청 크기 제한, 보안 헤더, 비밀번호 해시 |
| `common` | 공통 값 검증, DB 예외 응답, 업무 쓰기 잠금 |

요청은 보안 헤더 → 인증 → CSRF·요청 크기 검사 → Controller → Service → Repository
순서로 처리한다. Service는 Servlet API를 참조하지 않고 `CurrentUser`로 사용자 정보를 받는다.
인증 필터는 뒤따르는 요청 보호 필터의 예외도 JSON으로 반환하므로 필터 순서를 유지한다.

Repository는 Spring Data JPA의 `JpaRepository<Entity, Long>`을 사용한다. 조회 메서드·JPQL과
이슈 검색용 Specifications를 사용하며, 기존의 직접 JDBC·`Db.read/write`는 제거했다.
로그인 실패 테이블은 기존 테이블 그대로 Entity·PK 추가 없이 JPA Native Query로 사용한다.
과월호 담당 교환은 UNIQUE 제약과 항목 ID·완료 기록을 보존하기 위해 해당 작업만 Native Query를 사용한다.
로그인 실패 기록은 별도 Spring 트랜잭션에서 커밋한 뒤 바깥 인증 서비스가 401을 반환한다.
Spring Session JDBC는 로그인 세션 저장용으로 유지한다.

Entity는 `Long`·`BIGINT` PK와 IDENTITY 자동 생성, `LocalDate`·`DATE`, `Instant`·타임존 포함
TIMESTAMP, `boolean`·BOOLEAN을 사용한다. 고정 역할은 문자열 Enum으로 저장하며 연관관계는
단방향·지연 로딩 `@ManyToOne`을 기본으로 한다. 당직 연락처의 단일 행은 고정 Long ID 1을 사용한다.
사용자 `version`은 세션 무효화용이며 JPA `@Version` 필드가 아니다.
Entity를 HTTP 응답에 직접 반환하지 않는다. 기존 API의 0·1 플래그와 소문자 역할은 응답 DTO에서
유지한다. 입력 Map은 기존 검증과 PATCH의 필드 생략·null 구분을 보존한다.

Flyway V1·V2는 유지하며 V3 Java 마이그레이션이 `db/jpa-schema.sql`로 기존 업무 데이터를 새
스키마에 복사하고 행 수 확인 후 교체한다. 기존 숫자 ID는 보존하고 자연·복합 PK에는 Long ID를
추가한다. 새 IDENTITY 값은 보존한 최대 ID 다음 값부터 시작한다.
`ddl-auto=validate`와 `open-in-view=false`를 사용하므로 스키마 변경은 Flyway로만 수행한다.

## 최초 관리자

Flyway는 DB 스키마만 적용하며 계정을 생성하지 않는다. 새 DB에서는 DB 관리 도구로
`users`에 최초 관리자 계정을 직접 등록해야 한다. DB의 `role`은 `ADMIN`으로 지정하고,
`password_hash`에는 `Passwords`가 지원하는 형식의 비밀번호 해시를 저장한다.
기존 계정은 그대로 사용하며, 로컬 H2 파일을 삭제해 새로 시작하면 최초 계정도 다시 등록해야 한다.

커밋 제목은 기존 `이모지 태그 : 변경 내용` 형식을 따른다. 설정 `⚙️ Setting :`, 기능 `✨ Feature :`, 배포 `🌍 Ci :`, 수정 `🪲 Fix :`, 문서 `📄 Docs :`, 정리 `🔧 Chore :`, 개편 `🔨 Refactor :`를 쓴다.

## 확정된 근무 시간표와 주당 근무 시간

확정된 근무 시간표는 근무 전에 합의한 요일(월~금) 기준 표 1개로 `confirmed_shifts`에 저장한다. 매주 `POST /schedule/original/copy`로 대상 주의 `shifts`에 복사한다. 주당 근무 시간은 확정표로 계산하고, 근무자별 조정값은 주와 무관하게 `staff_hour_overrides`에 하나씩 둔다. `V5__confirmed_schedule.sql`이 기존 주별 원본 중 가장 최근 주를 요일 기준으로 옮기고 이전 테이블을 삭제한다. 실제 `shifts`의 수정·삭제는 확정표와 조정값을 바꾸지 않는다.

| 경로 (`/library-dashboard/api` 기준) | 동작 |
| --- | --- |
| `GET /schedule/original` | 확정표 조회. `shifts[].weekday`는 1(월)~5(금), 비어 있으면 `saved: false` |
| `PUT /schedule/original` | `{shifts: [{staff_id, weekday, start, end, role}]}`로 전체 교체 |
| `POST /schedule/original/copy` | `{target, overwrite}`. 대상 주에 근무가 있고 overwrite가 아니면 `{ok: false, existing}`. 퇴직자 근무는 빼고 `{ok: true, count, skipped}` |
| `GET /schedule/hours` | `{overrides: [...]}`로 조정값 조회 |
| `PUT /schedule/hours/{staff_id}` | `{total_minutes, desk_minutes, opens, closes}` 저장 |
| `DELETE /schedule/hours/{staff_id}` | 조정값을 지워 확정표 계산값으로 복원 |

조회는 로그인, 저장·복사·복원은 관리자 권한과 CSRF 토큰이 필요하다. 확정표는 09:00~22:00·30분 단위·같은 사람 같은 요일 겹침을 검증하고 한 트랜잭션으로 교체한다. 퇴직자의 기존 확정 근무는 유지할 수 있다. 시간 조정은 0~3900분의 30분 단위, 데스크 시간은 총 시간 이하, 오픈·마감은 각각 0~5회다.
