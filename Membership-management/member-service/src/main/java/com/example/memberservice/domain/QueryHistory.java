package com.example.memberservice.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("query_history")
public class QueryHistory {
    @Id
    private Long id;
    private String operation;    // "INSERT", "UPDATE", "DELETE", "SELECT"
    private Boolean success;     // 작업 성공 여부
    private LocalDateTime timestamp;  // 작업 시각
    private String details;      // 추가 상세 내용 (선택 사항)
}
