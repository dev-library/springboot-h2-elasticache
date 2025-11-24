package ac.gachon.elasticacheapp.dto;

import ac.gachon.elasticacheapp.entity.Order;
import ac.gachon.elasticacheapp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User 엔티티의 DTO - Hibernate 프록시 문제 완전 해결
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private Integer age;
    private List<OrderDto> orders;
    
    /**
     * User 엔티티를 DTO로 변환
     */
    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setAge(user.getAge());
        
        // orders를 OrderDto 리스트로 변환
        if (user.getOrders() != null) {
            dto.setOrders(
                user.getOrders().stream()
                    .map(OrderDto::from)
                    .collect(Collectors.toList())
            );
        }
        
        return dto;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDto implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private Long id;
        private String productName;
        private Integer price;
        private LocalDateTime orderDate;
        
        public static OrderDto from(Order order) {
            return new OrderDto(
                order.getId(),
                order.getProductName(),
                order.getPrice(),
                order.getOrderDate()
            );
        }
    }
}

