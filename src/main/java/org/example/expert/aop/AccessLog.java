package org.example.expert.aop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.expert.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "admin_access_log")
public class AccessLog {
//    userId, requestTime, requestUrl, requestBody
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    @Column(nullable = false)
    private String requestUrl;

    @Column
    private String requestBody;

    @Column
    private String responseBody;

    public AccessLog(User user, LocalDateTime requestTime, String requestUrl, String requestBody, String responseBody) {
        this.user = user;
        this.requestTime = requestTime;
        this.requestUrl = requestUrl;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
    }
}
