# 근무 대시보드 아키텍처

운영(prod): 브라우저 → 프론트 API 프록시 → HTTPS Ingress → ClusterIP Service:8080 → Spring Boot Pod → PostgreSQL.

로컬(local): 프론트 개발 프록시 → Spring Boot:8080 → 파일 H2(`data/library-dashboard.mv.db`). DB 컨테이너 없이 실행하며 업무 데이터와 JDBC 세션은 재시작 후에도 유지된다.

프론트와 백엔드는 독립 저장소다. 기본 `/library-dashboard` 경로와 기존 API 계약을 유지한다. 백엔드는 정적 화면을 제공하지 않는다. PostgreSQL이 업무 데이터와 로그인 세션을 보관한다. Pod 로컬 파일에는 영구 데이터를 쓰지 않는다.

두 환경 모두 Flyway가 같은 스키마를 적용하며 계정을 자동 생성하지 않는다. 새 DB의 최초 관리자는 DB에 직접 등록한다. 운영의 업무 쓰기는 PostgreSQL advisory transaction lock을 사용한다. 이 잠금은 DB 트랜잭션 종료 시 자동 해제되고 모든 Pod에 적용된다. 로컬 파일 H2는 단일 JVM에서 쓰기를 직렬화한다. 작은 팀의 짧은 쓰기를 직렬화해 시간표 겹침·주 복사·담당 변경·완료 처리의 검사와 저장을 함께 보호한다. 읽기는 repeatable read 트랜잭션으로 여러 쿼리에 같은 스냅샷을 사용한다.

Spring Session JDBC가 쿠키 세션을 공유한다. CSRF·권한 검사와 사용자 version 비교는 기존 방식이다. 비밀번호 해시 동시 계산은 Pod당 2개로 제한한다. 로그인 실패 횟수는 DB에 저장한다.

업무 데이터는 Spring Data JPA Repository와 Entity로 매핑하며 Service의 `@Transactional`로
트랜잭션을 관리한다. Entity는 Long PK, 날짜·시각 타입, boolean, 문자열 Enum과 지연 로딩
연관관계를 사용한다. 응답은 DTO로 변환해 기존 JSON을 유지하며 OSIV는 비활성화한다.
로그인 실패 기록은 커밋 뒤 인증 예외를 반환하도록 트랜잭션 서비스를 분리한다.
로그인 실패 테이블과 ID 보존이 필요한 과월호 담당 교환에는 JPA Native Query를 사용한다.

Flyway V3는 기존 데이터의 ID와 업무 기록을 보존하며 JPA 스키마로 변환한다. 구 버전과 새 버전은
동일 DB에서 병행 실행할 수 없으므로 Kubernetes Deployment는 Recreate로 배포하며 중단 시간이 있다.
외부 구 버전 프로세스도 중지한 뒤 적용하고, DB 스냅샷 없이 구 이미지로만 롤백하지 않는다.

liveness는 DB 상태와 분리하며 readiness에는 DB를 포함한다. 종료 시 진행 중인 요청을 최대 20초 기다린다. 각 Pod는 최대 5개 DB 연결을 사용한다. 전용 작업 큐·캐시·Kubernetes API 클라이언트는 추가하지 않았다.

실제 클러스터 운영은 PostgreSQL 및 배포 운영자의 책임이다. 앱은 업무 데이터 내보내기·복원 기능과 데이터 이관 도구를 제공하지 않는다. 상세 실행 순서는 [README.md](README.md)를 따른다.
