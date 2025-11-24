package ac.gachon.elasticacheapp.service;

import ac.gachon.elasticacheapp.dto.UserDto;
import ac.gachon.elasticacheapp.entity.User;
import ac.gachon.elasticacheapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CachedUserService {

    private final UserRepository repo;

    @Cacheable(value = "userCache", key = "#id")
    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        System.out.println(">>> Redis MISS → DB 조회 (JOIN FETCH)");
        // JOIN FETCH를 사용하여 User와 Orders를 한 번에 조회
        User user = repo.findByIdWithOrders(id)
                .orElseThrow(() -> new RuntimeException("Not Found"));
        
        System.out.println(">>> Orders count: " + user.getOrders().size());
        
        // DTO로 변환하여 반환 (Hibernate 프록시 완전 제거)
        return UserDto.from(user);
    }
}