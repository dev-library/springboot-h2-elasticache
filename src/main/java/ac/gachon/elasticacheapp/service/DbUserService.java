package ac.gachon.elasticacheapp.service;

import ac.gachon.elasticacheapp.entity.User;
import ac.gachon.elasticacheapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DbUserService {

    private final UserRepository repo;

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        System.out.println(">>> RDB HIT (JOIN FETCH)");
        // JOIN FETCH를 사용하여 User와 Orders를 한 번에 조회
        User user = repo.findByIdWithOrders(id)
                .orElseThrow(() -> new RuntimeException("Not Found"));
        
        // Orders를 명시적으로 로드
        user.getOrders().size();
        
        System.out.println(">>> Orders count: " + user.getOrders().size());
        
        return user;
    }
}