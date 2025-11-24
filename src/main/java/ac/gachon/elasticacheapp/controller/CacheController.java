package ac.gachon.elasticacheapp.controller;

import ac.gachon.elasticacheapp.dto.UserDto;
import ac.gachon.elasticacheapp.dto.UserResponse;
import ac.gachon.elasticacheapp.service.CachedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CachedUserService service;
    private final CacheManager cacheManager;

    @GetMapping("/{id}")
    public UserResponse getFromCache(@PathVariable Long id) {
        
        long start = System.currentTimeMillis();
        
        // 캐시 히트 여부 확인 (service 호출 전)
        Cache cache = cacheManager.getCache("userCache");
        boolean isCacheHit = (cache != null && cache.get(id) != null);
        
        UserDto userDto = service.getUser(id);
        long end = System.currentTimeMillis();
        
        long processingTime = end - start;
        String cacheStatus = isCacheHit ? "REDIS_HIT" : "REDIS_MISS";

        System.out.println("[CACHE] 처리 시간: " + processingTime + " ms (" + cacheStatus + ")");

        return new UserResponse(userDto, processingTime, cacheStatus);
    }
}