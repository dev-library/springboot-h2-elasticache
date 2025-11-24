package ac.gachon.elasticacheapp.controller;

import ac.gachon.elasticacheapp.entity.User;
import ac.gachon.elasticacheapp.service.CachedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CachedUserService service;

    @GetMapping("/{id}")
    public User getFromCache(@PathVariable Long id) {

        long start = System.currentTimeMillis();
        User user = service.getUser(id);
        long end = System.currentTimeMillis();

        System.out.println("[CACHE] 처리 시간: " + (end - start) + " ms");

        return user;
    }
}