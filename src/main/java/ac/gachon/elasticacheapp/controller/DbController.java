package ac.gachon.elasticacheapp.controller;

import ac.gachon.elasticacheapp.entity.User;
import ac.gachon.elasticacheapp.service.DbUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/db")
@RequiredArgsConstructor
public class DbController {

    private final DbUserService service;

    @GetMapping("/{id}")
    public User getFromDb(@PathVariable Long id) {

        long start = System.currentTimeMillis();
        User user = service.getUser(id);
        long end = System.currentTimeMillis();

        System.out.println("[RDB] 처리 시간: " + (end - start) + " ms");

        return user;
    }
}
