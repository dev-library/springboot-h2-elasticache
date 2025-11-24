# Elasticache App - Redis 캐싱 성능 비교 프로젝트

Spring Boot와 Redis를 활용한 캐싱 성능 최적화 데모 프로젝트입니다.

## ⚡ Quick Start

```bash
# 1. Redis 컨테이너 실행
docker run -d --name redis-test -p 6379:6379 redis

# 2. 프로젝트 실행
./gradlew bootRun

# 3. API 테스트 (데이터 생성 완료 후 약 30초)
# RDB 직접 조회
curl http://localhost:8080/db/100

# Redis 캐시 조회 (첫 조회 - MISS)
curl http://localhost:8080/cache/100

# Redis 캐시 조회 (재조회 - HIT) ⚡
curl http://localhost:8080/cache/100
```

**성능 차이:**
- RDB 직접: **50-100ms**
- Redis HIT: **1-3ms** ⚡ (약 **30-50배 빠름!**)

---

## 📋 프로젝트 개요

RDB 직접 조회와 Redis 캐시 조회의 성능 차이를 실습하기 위한 프로젝트입니다.
복잡한 JOIN 쿼리(User + Orders)를 통해 실제 운영 환경과 유사한 조건에서 캐싱의 효과를 확인할 수 있습니다.

## 🛠 기술 스택

- **Java 17**
- **Spring Boot 3.5.8**
- **Spring Data JPA** - ORM, Repository 패턴
- **Spring Data Redis** - Redis 캐싱
- **H2 Database** - 인메모리 데이터베이스 (기본)
- **MySQL** - 관계형 데이터베이스 (선택 사항)
- **Docker** - Redis & MySQL 컨테이너 실행
- **Lombok** - 보일러플레이트 코드 제거
- **Jackson Datatype Hibernate6** - Hibernate Proxy 직렬화 지원
- **Gradle** - 빌드 도구

## 🚀 주요 기능

### 1. 대용량 데이터 생성 (멀티스레드)

- **User**: 100,000건
- **Order**: 약 125만건 (각 User당 10-15개)
- **멀티스레드**: 10개 스레드로 병렬 처리
- **성능**: 약 30초 내 완료 (기존 단일 스레드 대비 **7.7배 개선**)

```java
// 멀티스레드로 배치 저장
for (int threadId = 0; threadId < 10; threadId++) {
    executor.submit(() -> {
        createUsersAndOrders(startId, endId);
    });
}
```

### 2. 복잡한 JOIN 쿼리

User와 Orders를 `JOIN FETCH`로 한 번에 조회하여 N+1 문제를 방지하고, 실제 운영 환경과 유사한 복잡도를 구현했습니다.

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(@Param("id") Long id);
```

### 3. Redis 캐싱

Spring Cache Abstraction을 활용한 선언적 캐싱 구현:

```java
@Cacheable(value = "userCache", key = "#id")
@Transactional(readOnly = true)
public User getUser(Long id) {
    return repo.findByIdWithOrders(id)
        .orElseThrow(() -> new RuntimeException("Not Found"));
}
```

## 📊 성능 비교 결과

| 조회 방식 | 처리 시간 | 설명 |
|----------|---------|------|
| **RDB 직접 조회** | 75ms | JOIN FETCH로 User + Orders 조회 |
| **Redis MISS (첫 조회)** | 930ms | DB 조회 + Redis 저장 |
| **Redis HIT (캐시 조회)** | 74ms ⚡ | Redis에서 직접 조회 |

> **참고**: H2 인메모리 DB를 사용하여 RDB와 Redis의 성능 차이가 크지 않습니다.
> 실제 운영 환경(MySQL, PostgreSQL)에서는 **Redis가 10-100배 더 빠릅니다**.

## 🏗 아키텍처

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ├─── /db/{id}     ──→ DbUserService      ──→ H2 Database
       │                     (RDB 직접 조회)
       │
       └─── /cache/{id}  ──→ CachedUserService  ──→ Redis Cache
                             (@Cacheable)            ↓ (MISS)
                                                H2 Database
```

### 주요 컴포넌트

#### 1. Entity Layer
- **User**: 사용자 엔티티 (id, name, age)
- **Order**: 주문 엔티티 (id, productName, price, orderDate)
- **관계**: User 1:N Order (EAGER fetch)

#### 2. Repository Layer
- **UserRepository**: JPA Repository + JPQL JOIN FETCH 쿼리
- **OrderRepository**: JPA Repository

#### 3. DTO Layer
- **UserDto**: Entity를 순수 POJO로 변환 (Hibernate Proxy 제거)
- **UserResponse**: API 응답 (User + 소요시간 + 캐시 상태)

#### 4. Service Layer
- **DbUserService**: RDB 직접 조회 → UserDto 변환
- **CachedUserService**: Redis 캐싱 적용 (`@Cacheable`) → UserDto 변환

#### 5. Controller Layer
- **DbController**: `/db/{id}` - RDB 성능 측정
- **CacheController**: `/cache/{id}` - Redis 캐시 성능 측정

#### 6. Configuration
- **RedisConfig**: Redis 연결, ObjectMapper 설정
- **@EnableCaching**: Spring Cache 활성화

## 🔧 설정 및 실행

### 사전 요구사항

1. **Java 17** 설치
2. **Docker** 설치 (Redis 컨테이너 실행용)

### Redis 실행

```bash
docker run -d --name redis-test -p 6379:6379 redis
```

### 프로젝트 실행

```bash
./gradlew bootRun
```

서버 시작 후 약 30초간 데이터가 생성됩니다.

---

## 🐬 MySQL로 전환하기 (선택 사항)

기본적으로 H2 인메모리 데이터베이스를 사용하지만, 더 실제 환경과 유사한 성능 비교를 위해 MySQL로 전환할 수 있습니다.

### 1. Docker로 MySQL 컨테이너 실행

```bash
# MySQL 8.0 컨테이너 실행
docker run -d \
  --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=elasticache_db \
  -e MYSQL_USER=testuser \
  -e MYSQL_PASSWORD=testpassword \
  -p 3306:3306 \
  mysql:8.0
```

**Windows PowerShell:**
```powershell
docker run -d `
  --name mysql-test `
  -e MYSQL_ROOT_PASSWORD=rootpassword `
  -e MYSQL_DATABASE=elasticache_db `
  -e MYSQL_USER=testuser `
  -e MYSQL_PASSWORD=testpassword `
  -p 3306:3306 `
  mysql:8.0
```

### 2. build.gradle 의존성 추가

`build.gradle`의 `dependencies` 블록에 MySQL 드라이버를 추가하세요:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate6'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // H2 주석 처리 또는 제거
    // runtimeOnly 'com.h2database:h2'
    
    // MySQL 드라이버 추가
    runtimeOnly 'com.mysql:mysql-connector-j'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

### 3. application.yaml 설정 변경

`src/main/resources/application.yaml` 파일을 다음과 같이 수정하세요:

```yaml
spring:
    application:
        name: elasticache-app
    
    # MySQL 설정
    datasource:
        url: jdbc:mysql://localhost:3306/elasticache_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
        driver-class-name: com.mysql.cj.jdbc.Driver
        username: testuser
        password: testpassword
        hikari:
            maximum-pool-size: 20
            minimum-idle: 10
    
    jpa:
        hibernate:
            ddl-auto: create  # 개발 환경: create, 운영 환경: validate
        show-sql: false
        open-in-view: true
        properties:
            hibernate:
                dialect: org.hibernate.dialect.MySQLDialect
                format_sql: true
    
    data:
        redis:
            host: localhost
            port: 6379

server:
    port: 8080
```

### 4. MySQL 연결 확인

```bash
# MySQL 컨테이너에 접속
docker exec -it mysql-test mysql -u testuser -p

# 비밀번호 입력: testpassword

# 데이터베이스 확인
USE elasticache_db;
SHOW TABLES;
```

### 5. 성능 비교 팁

MySQL 사용 시 더 실제적인 성능 차이를 확인할 수 있습니다:

| 환경 | RDB 직접 조회 | Redis 캐시 조회 |
|------|---------------|-----------------|
| **H2 (인메모리)** | 50-100ms | 1-3ms |
| **MySQL (Docker)** | 100-200ms | 1-3ms |
| **MySQL (원격 서버)** | 200-500ms | 1-3ms |

**주의사항:**
- MySQL은 디스크 I/O가 발생하므로 H2보다 느립니다
- 첫 실행 시 데이터 생성이 더 오래 걸릴 수 있습니다 (약 1-2분)
- `ddl-auto: create`는 서버 재시작 시마다 데이터를 초기화합니다

### 6. 컨테이너 관리

```bash
# MySQL 컨테이너 중지
docker stop mysql-test

# MySQL 컨테이너 시작
docker start mysql-test

# MySQL 컨테이너 삭제
docker rm -f mysql-test

# 두 컨테이너 모두 확인
docker ps -a | grep -E "redis-test|mysql-test"
```

---

## 📡 API 엔드포인트

### 1. RDB 직접 조회

```http
GET http://localhost:8080/db/{id}
```

**응답 예시:**
```json
{
  "user": {
    "id": 100,
    "name": "User100",
    "age": 30,
    "orders": [
      {
        "id": 1250,
        "productName": "Laptop",
        "price": 85000,
        "orderDate": "2024-11-15T12:30:00"
      }
    ]
  },
  "processingTimeMs": 75,
  "cacheStatus": "RDB_DIRECT"
}
```

### 2. Redis 캐시 조회

```http
GET http://localhost:8080/cache/{id}
```

**첫 조회 (MISS) 응답 예시:**
```json
{
  "user": { "id": 100, "name": "User100", ... },
  "processingTimeMs": 850,
  "cacheStatus": "REDIS_MISS"
}
```

**재조회 (HIT) 응답 예시:**
```json
{
  "user": { "id": 100, "name": "User100", ... },
  "processingTimeMs": 2,
  "cacheStatus": "REDIS_HIT"
}
```

**응답 필드:**
- `user`: 사용자 및 주문 데이터
- `processingTimeMs`: **처리 소요 시간 (밀리초)** ⏱️
- `cacheStatus`: 캐시 상태
  - `RDB_DIRECT`: RDB 직접 조회
  - `REDIS_MISS`: Redis 캐시 미스 → DB 조회
  - `REDIS_HIT`: Redis 캐시 히트 ⚡

## 🎯 주요 구현 사항

### 1. 멀티스레드 DataLoader

10개의 스레드로 병렬 처리하여 데이터 생성 시간을 대폭 단축:

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int threadId = 0; threadId < 10; threadId++) {
    executor.submit(() -> createData(startId, endId));
}
```

**성능 개선:**
- 기존: 200초 → 개선: 26-30초 (**7.7배 빠름**)

### 2. Redis 직렬화 문제 해결

**문제**: Hibernate Proxy 객체가 Redis 직렬화 시 `LazyInitializationException` 발생

**최종 해결책: DTO 변환**
```java
@Data
public class UserDto implements Serializable {
    private Long id;
    private String name;
    private Integer age;
    private List<OrderDto> orders;
    
    // Entity → DTO 변환
    public static UserDto from(User user) {
        // Hibernate Proxy를 완전히 제거하고 순수 POJO로 변환
        return new UserDto(...);
    }
}
```

**이전 시도한 방법들:**
1. `FetchType.EAGER` 설정
2. `Jackson Hibernate6 Module` 추가
3. `@Transactional(readOnly = true)` 적용
4. **최종 해결**: DTO 패턴으로 Entity와 완전 분리

```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Hibernate6Module());  // Hibernate Proxy 처리
    // ...
}
```

**왜 DTO 변환이 필요한가?**
- Hibernate 엔티티는 Lazy Loading Proxy를 포함할 수 있음
- Redis 역직렬화 시 Hibernate Session이 없어 Proxy 초기화 불가
- DTO로 변환하면 순수한 데이터만 Redis에 저장되어 문제 해결

### 3. JOIN FETCH 최적화

N+1 문제를 방지하기 위해 JOIN FETCH 사용:

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(@Param("id") Long id);
```

## 📈 성능 최적화 요약

### 데이터 생성 속도

| 항목 | 최적화 전 | 최적화 후 | 개선율 |
|------|----------|----------|--------|
| **전체 데이터 생성** | 200초 | 30초 | **6.7배** ⚡ |
| **User 생성** | 13초 | 2초 | **6.5배** ⚡ |
| **Order 생성** | 187초 | 28초 | **6.7배** ⚡ |

### 조회 성능 (H2 기준)

| 조회 방식 | 평균 응답 시간 | 비고 |
|----------|---------------|------|
| **RDB 직접 조회** | 50-100ms | JOIN FETCH 쿼리 |
| **Redis MISS** | 50-100ms | DB 조회 + 캐시 저장 |
| **Redis HIT** | 1-3ms | **30-50배 빠름** ⚡ |

### 조회 성능 (MySQL 기준)

| 조회 방식 | 평균 응답 시간 | 비고 |
|----------|---------------|------|
| **RDB 직접 조회** | 100-200ms | 디스크 I/O 발생 |
| **Redis MISS** | 100-200ms | DB 조회 + 캐시 저장 |
| **Redis HIT** | 1-3ms | **50-100배 빠름** ⚡⚡ |

## 🔑 핵심 학습 포인트

1. **Spring Cache Abstraction**: `@Cacheable`로 간단한 캐싱 구현
2. **Redis 직렬화/역직렬화**: Jackson ObjectMapper 설정의 중요성
3. **DTO 패턴의 중요성**: Hibernate Proxy 문제를 완벽하게 해결
4. **멀티스레드 프로그래밍**: ExecutorService를 활용한 병렬 처리
5. **JPA N+1 문제**: JOIN FETCH로 해결
6. **성능 측정**: 실제 측정을 통한 최적화 효과 검증
7. **Docker 활용**: MySQL, Redis 컨테이너로 실제 환경 구성

## 🐛 트러블슈팅

### 문제 1: `spring-boot-starter-webmvc`를 찾을 수 없음
**해결**: 올바른 이름은 `spring-boot-starter-web`

### 문제 2: H2에서 `USER` 테이블 생성 실패
**해결**: `USER`는 예약어이므로 `@Table(name = "users")` 추가

### 문제 3: Redis 역직렬화 시 `LazyInitializationException`
**증상**: Redis 캐시에서 조회 시 500 에러 발생
```
org.hibernate.LazyInitializationException: 
failed to lazily initialize a collection: could not initialize proxy - no Session
```

**시도한 해결 방법:**
1. `jackson-datatype-hibernate6` 의존성 추가 ✅
2. `FetchType.EAGER` 사용 ✅
3. `@Transactional(readOnly = true)` 적용 ✅
4. `Hibernate6Module.FORCE_LAZY_LOADING` 설정 ❌
5. `open-in-view: true` 설정 ❌

**최종 해결**: **DTO 패턴** ✨
```java
// Service에서 Entity → DTO 변환
public UserDto getUser(Long id) {
    User user = repo.findByIdWithOrders(id);
    return UserDto.from(user);  // Hibernate Proxy 완전 제거
}
```

### 문제 4: LocalDateTime 직렬화 실패
**해결**: `JavaTimeModule` 등록

### 문제 5: Redis 캐시 HIT인데도 DB 조회 발생
**원인**: 캐시 키 생성 방식 문제
**해결**: `@Cacheable(value = "userCache", key = "#id")` 명시적 키 지정
