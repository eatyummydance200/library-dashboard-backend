# 근무 대시보드 아키텍처

운영(prod): 브라우저 → 프론트 API 프록시 → HTTPS Ingress → ClusterIP Service:8080 → Spring Boot Pod → PostgreSQL.

로컬(local): 프론트 개발 프록시 → Spring Boot:8080 → 파일 H2(`data/library-dashboard.mv.db`). DB 컨테이너 없이 실행하며 업무 데이터와 JDBC 세션은 재시작 후에도 유지된다. Docker Compose로 실행하면 같은 파일을 `local-data` 볼륨에 보관한다.

프론트와 백엔드는 독립 저장소다. 기본 `/library-dashboard` 경로와 기존 API 계약을 유지한다. 백엔드는 정적 화면을 제공하지 않는다. PostgreSQL이 업무 데이터와 로그인 세션을 보관한다. Pod 로컬 파일에는 영구 데이터를 쓰지 않는다.

두 환경 모두 Flyway가 같은 스키마를 적용한 뒤 빈 DB의 최초 관리자를 만든다. 운영의 최초 관리자 생성과 업무 쓰기는 같은 PostgreSQL advisory transaction lock을 사용한다. 이 잠금은 DB 트랜잭션 종료 시 자동 해제되고 모든 Pod에 적용된다. 로컬 파일 H2는 단일 JVM에서 쓰기를 직렬화한다. 작은 팀의 짧은 쓰기를 직렬화해 시간표 겹침·주 복사·담당 변경·완료 처리의 검사와 저장을 함께 보호한다. 읽기는 repeatable read 트랜잭션으로 여러 쿼리에 같은 스냅샷을 사용한다.

Spring Session JDBC가 쿠키 세션을 공유한다. CSRF·권한 검사와 사용자 version 비교는 기존 방식이다. 비밀번호 해시 동시 계산은 Pod당 2개로 제한한다. 로그인 실패 횟수는 DB에 저장한다.

liveness는 DB 상태와 분리하며 readiness에는 DB를 포함한다. 종료 시 진행 중인 요청을 최대 20초 기다린다. 각 Pod는 최대 5개 DB 연결을 사용한다. 전용 작업 큐·캐시·Kubernetes API 클라이언트는 추가하지 않았다.

백업과 실제 클러스터 운영은 PostgreSQL 및 배포 운영자의 책임이다. API는 일관된 업무 데이터 JSON을 내보낸다. 이관 도구는 원본 SQLite v7 또는 JSON을 새 DB에만 가져온다. 상세 실행·복구 순서는 [README.md](README.md)를 따른다.
