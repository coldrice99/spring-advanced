package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j(topic = "AccessLogger")
@Aspect
@Component
@RequiredArgsConstructor
public class AccessLoggingAop {

    private final HttpServletRequest request;
    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;

    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.*(..))")
    private void comment() {}

    @Pointcut("execution(* org.example.expert.domain.user.controller.UserAdminController.*(..))")
    private void user() {}

    @After("comment() || user()")
    public Object logAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // 요청 시각, URL, 본문 정보 수집
        LocalDateTime requestTime = LocalDateTime.now();
        String requestUrl = request.getRequestURI();
        String requestBody = extractRequestBody();

        // 필터에서 설정된 사용자 ID 가져오기
        Long userId = (Long) request.getAttribute("userId");
        Optional<User> user = userRepository.findById(userId);

        // 유저가 없을 경우 로그 경고 후 종료
        if (user.isEmpty()) {
            log.warn("User not found for ID: {}", userId);
            return joinPoint.proceed();
        }

        // 메서드 실행 후 응답 로깅
        Object response = joinPoint.proceed();
        String responseBody = response != null ? response.toString() : "No Response Body";

        log.info("요청한 사용자의 ID: {}, API 요청 시각: {}, API 요청 URL: {}, Request Body: {}, Response Body: {}",
                userId, requestTime, requestUrl, requestBody, responseBody);

        // 접근 로그 저장
        accessLogRepository.save(new AccessLog(user.get(), requestTime, requestUrl, requestBody, responseBody));

        return response;
    }

    private String extractRequestBody() {
        if (request instanceof ContentCachingRequestWrapper cachingRequest) {
            byte[] contentAsByteArray = cachingRequest.getContentAsByteArray();
            return new String(contentAsByteArray, StandardCharsets.UTF_8);
        }
        return "No Request Body";
    }
}
