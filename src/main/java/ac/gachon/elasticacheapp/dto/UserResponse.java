package ac.gachon.elasticacheapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    
    private UserDto user;
    
    private Long processingTimeMs;
    
    private String cacheStatus;  // "RDB_DIRECT", "REDIS_MISS", "REDIS_HIT"
}

