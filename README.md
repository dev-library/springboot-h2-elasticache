# Elasticache App - Redis 캐싱 성능 비교 프로젝트

Spring Boot와 Redis를 활용한 캐싱 성능 최적화 데모 프로젝트입니다.

## 📋 프로젝트 개요

RDB 직접 조회와 Redis 캐시 조회의 성능 차이를 실습하기 위한 프로젝트입니다.
복잡한 JOIN 쿼리(User + Orders)를 통해 실제 운영 환경과 유사한 조건에서 캐싱의 효과를 확인할 수 있습니다.

## 🛠 기술 스택

- **Java 17**
- **Spring Boot 3.5.8**
- **Spring Data JPA** - ORM, Repository 패턴
- **Spring Data Redis** - Redis 캐싱
- **H2 Database** - 인메모리 데이터베이스
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

#### 3. Service Layer
- **DbUserService**: RDB 직접 조회 (캐싱 없음)
- **CachedUserService**: Redis 캐싱 적용 (`@Cacheable`)

#### 4. Controller Layer
- **DbController**: `/db/{id}` - RDB 성능 측정
- **CacheController**: `/cache/{id}` - Redis 캐시 성능 측정

#### 5. Configuration
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

## 📡 API 엔드포인트

### 1. RDB 직접 조회

```http
GET http://localhost:8080/db/{id}
```

**응답 예시:**
```json
{
  "id": 1,
  "name": "User1",
  "age": 21,
  "orders": [
    {
      "id": 1,
      "productName": "Laptop",
      "price": 85000,
      "orderDate": "2025-03-15T12:30:00"
    }
  ]
}
```

### 2. Redis 캐시 조회

```http
GET http://localhost:8080/cache/{id}
```

- **첫 조회(MISS)**: DB에서 가져와서 Redis에 저장
- **재조회(HIT)**: Redis에서 직접 반환 (빠름!)

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

**해결책:**
1. `FetchType.EAGER` 설정
2. `Jackson Hibernate6 Module` 추가
3. `@Transactional(readOnly = true)` 적용

```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Hibernate6Module());  // Hibernate Proxy 처리
    // ...
}
```

### 3. JOIN FETCH 최적화

N+1 문제를 방지하기 위해 JOIN FETCH 사용:

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(@Param("id") Long id);
```

## 📈 성능 최적화 요약

| 항목 | 최적화 전 | 최적화 후 | 개선율 |
|------|----------|----------|--------|
| **데이터 생성 시간** | 200초 | 30초 | **6.7배** ⚡ |
| **User 생성** | 13초 | 2초 | **6.5배** ⚡ |
| **캐시 조회 (HIT)** | - | 74ms | **12배 빠름** (vs MISS 930ms) |

## 🔑 핵심 학습 포인트

1. **Spring Cache Abstraction**: `@Cacheable`로 간단한 캐싱 구현
2. **Redis 직렬화/역직렬화**: Jackson ObjectMapper 설정의 중요성
3. **멀티스레드 프로그래밍**: ExecutorService를 활용한 병렬 처리
4. **JPA N+1 문제**: JOIN FETCH로 해결
5. **성능 측정**: 실제 측정을 통한 최적화 효과 검증

## 🐛 트러블슈팅

### 문제 1: `spring-boot-starter-webmvc`를 찾을 수 없음
**해결**: 올바른 이름은 `spring-boot-starter-web`

### 문제 2: H2에서 `USER` 테이블 생성 실패
**해결**: `USER`는 예약어이므로 `@Table(name = "users")` 추가

### 문제 3: Redis 역직렬화 시 `LazyInitializationException`
**해결**: 
- `jackson-datatype-hibernate6` 의존성 추가
- `FetchType.EAGER` 사용
- `@Transactional(readOnly = true)` 적용

### 문제 4: LocalDateTime 직렬화 실패
**해결**: `JavaTimeModule` 등록
