

# 프로젝트 개요

이 프로젝트는 **회원관리 프로그램**으로, 두 개의 마이크로서비스로 구성되어 있습니다.

- **info-service:**  
  - 실제 회원 데이터(Member 엔티티)를 관리하며, CRUD API를 제공하는 서비스입니다.
  - MySQL 데이터베이스(info_service_db)에 회원 정보가 저장됩니다.

- **member-service:**  
  - info-service의 CRUD API를 호출하여 회원 데이터 작업을 수행하고, 그 결과(삽입, 조회, 수정, 삭제 등)의 이력을 별도의 데이터베이스(member_service_db)의 `query_history` 테이블에 기록하는 서비스입니다.
  - Reactive WebClient와 Spring WebFlux, Spring Data R2DBC를 사용하여 비동기 방식으로 동작합니다.

# 프로젝트 구성

## 1. info-service

- **역할:** 회원 데이터의 CRUD 작업을 수행  
- **주요 구성 요소:**  
  - `Member` 엔티티 (식별자 `@Id` 포함)  
  - Spring Data JDBC를 사용한 CRUD 리포지토리  
  - REST 컨트롤러  
  - MySQL 데이터베이스 (info_service_db)와 연동  
- **포트:** 기본적으로 8888 (또는 환경에 맞게 변경 가능)

## 2. member-service

- **역할:** info-service의 CRUD API를 호출하여, 처리 결과에 따라 작업 이력(History)을 별도의 데이터베이스(member_service_db)의 `query_history` 테이블에 저장  
- **주요 구성 요소:**  
  - Reactive WebClient를 이용한 info-service 호출  
  - `QueryHistory` 엔티티 (식별자 `@Id` 포함)  
  - Spring Data R2DBC를 사용한 Reactive CRUD 리포지토리  
  - REST 컨트롤러를 통해 외부 요청을 받고, info-service에 요청 후 결과에 따라 이력 저장  
- **포트:** 기본적으로 8080 (Docker 또는 환경에 따라 포트 매핑 가능)
- **WebClient 설정:** 별도의 `WebClientConfig` 클래스를 통해 빈으로 등록
- **환경 변수:**  
  - `INFO_SERVICE_URL` 환경 변수로 info-service의 기본 URL을 설정할 수 있으며, 기본값은 `http://localhost:8888`로 설정되어 있습니다.

# 주요 코드 구성

## MemberService (member-service)
MemberService에서는 info-service의 CRUD API 호출 후, 성공 또는 오류 여부에 따라 `QueryHistory`를 저장합니다.  
아래는 최종 버전의 `MemberService` 코드 예시입니다.

```java
package com.example.memberservice.service;

import com.example.memberservice.domain.QueryHistory;
import com.example.memberservice.repository.QueryHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
public class MemberService {
    private final WebClient webClient;
    private final QueryHistoryRepository historyRepository;

    // info-service URL은 환경변수 또는 application.yml 등에서 주입받도록 처리
    private final String infoServiceUrl = System.getenv("INFO_SERVICE_URL") != null
            ? System.getenv("INFO_SERVICE_URL")
            : "http://localhost:8888";

    public MemberService(WebClient webClient, QueryHistoryRepository historyRepository) {
        this.webClient = webClient;
        this.historyRepository = historyRepository;
    }

    public Mono<String> createMember(Object memberData) {
        return webClient.post()
                .uri(infoServiceUrl + "/members")
                .bodyValue(memberData)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                    saveHistory("INSERT", true, "Created member successfully")
                        .thenReturn(response)
                )
                .onErrorResume(e ->
                    saveHistory("INSERT", false, e.getMessage())
                        .then(Mono.error(e))
                );
    }

    public Mono<String> getMember(Long id) {
        return webClient.get()
                .uri(infoServiceUrl + "/members/" + id)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                    saveHistory("SELECT", true, "Retrieved member with id: " + id)
                        .thenReturn(response)
                )
                .onErrorResume(e ->
                    saveHistory("SELECT", false, e.getMessage())
                        .then(Mono.error(e))
                );
    }

    public Mono<String> updateMember(Long id, Object memberData) {
        return webClient.put()
                .uri(infoServiceUrl + "/members/" + id)
                .bodyValue(memberData)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                    saveHistory("UPDATE", true, "Updated member with id: " + id)
                        .thenReturn(response)
                )
                .onErrorResume(e ->
                    saveHistory("UPDATE", false, e.getMessage())
                        .then(Mono.error(e))
                );
    }

    public Mono<String> deleteMember(Long id) {
        return webClient.delete()
                .uri(infoServiceUrl + "/members/" + id)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                    saveHistory("DELETE", true, "Deleted member with id: " + id)
                        .thenReturn(response)
                )
                .onErrorResume(e ->
                    saveHistory("DELETE", false, e.getMessage())
                        .then(Mono.error(e))
                );
    }

    private Mono<Void> saveHistory(String operation, boolean success, String details) {
        QueryHistory history = new QueryHistory();
        history.setOperation(operation);
        history.setSuccess(success);
        history.setTimestamp(LocalDateTime.now());
        history.setDetails(details);
        return historyRepository.save(history).then();
    }
}
```

## WebClientConfig (member-service)
```java
package com.example.memberservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }
}
```

## QueryHistory 도메인 및 Repository (member-service)
```java
package com.example.memberservice.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("query_history")
public class QueryHistory {
    @Id
    private Long id;
    private String operation;
    private Boolean success;
    private LocalDateTime timestamp;
    private String details;
}
```

```java
package com.example.memberservice.repository;

import com.example.memberservice.domain.QueryHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface QueryHistoryRepository extends ReactiveCrudRepository<QueryHistory, Long> {
}
```

# 도커 및 배포

## Dockerfile 예시 (member-service)
```dockerfile
FROM openjdk:21
WORKDIR /app
COPY build/libs/member-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## docker-compose.yml 예시
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 1234
      MYSQL_DATABASE: info_service_db
    ports:
      - "3308:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  info-service:
    build:
      context: ../Membership-management/info-service
      dockerfile: Dockerfile
    ports:
      - "8888:8888"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/info_service_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=1234
    depends_on:
      mysql:
        condition: service_healthy

  member-service:
    build:
      context: ../Membership-management/member-service
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_R2DBC_URL=r2dbc:mysql://root:1234@mysql:3306/member_service_db?useSSL=false&allowPublicKeyRetrieval=true
      - INFO_SERVICE_URL=http://info-service:8888
    depends_on:
      - info-service

volumes:
  mysql_data:
```

# 실행 및 테스트 방법

1. **빌드 및 배포:**  
   - 각 서비스 (info-service, member-service)에서 `./gradlew clean build -x test` 명령을 실행하여 JAR 파일을 생성합니다.
   - `docker-compose up --build -d` 명령을 실행하여 모든 컨테이너를 빌드 및 실행합니다.

2. **테스트:**  
   - Postman을 사용하여 member-service의 엔드포인트(예: `http://localhost:8080/members`)로 요청을 보냅니다.
   - member-service가 info-service의 CRUD API를 호출하고, 결과에 따라 `query_history` 테이블에 작업 이력이 저장됩니다.
   - 또한 info-service의 데이터베이스(info_service_db)의 member 테이블에 회원 데이터가 저장되는지 확인합니다.
   - 직접 MySQL 클라이언트를 통해 `SELECT * FROM query_history;` 쿼리를 실행하여 이력이 기록되었는지 확인합니다.

3. **로그 확인:**  
   - 도커 컨테이너 로그 (`docker logs <container-name>`)를 통해 애플리케이션의 실행 상태와 디버그 로그를 확인합니다.

# 참고 사항

- member-service는 info-service의 API를 호출하여 CRUD 결과에 따라 이력을 저장하므로, **member-service의 엔드포인트로 요청을 보내야** history가 저장됩니다.
- 개발 단계에서는 도커 없이 개별 실행(IDE 또는 로컬 서버 실행)하여 빠른 피드백을 받을 수 있으며, 최종 통합 테스트는 도커 환경에서 진행하는 것이 좋습니다.
- Reactive 프로그래밍 특성상, 비동기 체인의 로그 출력이나 오류 처리는 별도의 디버깅 로직이 필요할 수 있습니다.

---
