package ac.gachon.elasticacheapp.controller;

import ac.gachon.elasticacheapp.dto.UserDto;
import ac.gachon.elasticacheapp.dto.UserResponse;
import ac.gachon.elasticacheapp.service.DbUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/db")
@RequiredArgsConstructor
public class DbController {

    private final DbUserService service;

    @GetMapping("/{id}")
    public UserResponse getFromDb(@PathVariable Long id) {

        long start = System.currentTimeMillis();
        UserDto userDto = service.getUser(id);
        long end = System.currentTimeMillis();
        
        long processingTime = end - start;

        System.out.println("[RDB] 처리 시간: " + processingTime + " ms");

        return new UserResponse(userDto, processingTime, "RDB_DIRECT");
    }
}
