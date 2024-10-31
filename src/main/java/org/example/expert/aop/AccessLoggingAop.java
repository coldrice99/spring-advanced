package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.expert.config.JwtUtil;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j(topic = "UseTimeAop")
@Aspect
@Component
@RequiredArgsConstructor
public class AccessLoggingAop {

    private final HttpServletRequest request;
    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;

    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.*(..))")
    private void comment() {
    }

    @Pointcut("execution(* org.example.expert.domain.user.controller.UserAdminController.*(..))")
    private void user() {
    }

    @Around("comment() || user()")
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        // 요청 시각 시록
        LocalDateTime requestTime = LocalDateTime.now();

        // 요청 정보 수집
        String requestUrl = request.getRequestURI();
        String requestBody = getRequestBody();

        // 필터에서 설정된 사용자 ID 가져오기
        Long userId = (Long) request.getAttribute("userId");
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("User not found for ID: {}", userId);
            return joinPoint.proceed();  // 유저가 없는 경우 요청을 진행하되, 기록은 생략
        }

        log.info("요청한 사용자의 ID : {}, API 요청 시각 : {}, API 요청 URL : {}, Request Body : {}",
                userId, requestTime, requestUrl, requestBody );

        //메서드 실행 및 응답 기록
        Object response = joinPoint.proceed(); // 실제 메서드 실행

        // 응답 본문 로깅
        String responseBody = response != null ? response.toString() : "No Response Body";
        log.info("Response Body : {}", responseBody);

        // 접근 기록 저장
        AccessLog accessLog = new AccessLog(user.get(), requestTime, requestUrl, requestBody, responseBody);
        accessLogRepository.save(accessLog);

        return response;
    }

    private String getRequestBody() {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = request.getReader();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            log.error("Failed to read request body", e);
            return "Failed to read request body";
        }
    }

}
