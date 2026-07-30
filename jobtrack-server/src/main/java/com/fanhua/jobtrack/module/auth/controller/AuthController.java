package com.fanhua.jobtrack.module.auth.controller;

import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.config.properties.AuthProperties;
import com.fanhua.jobtrack.module.auth.dto.LoginRequest;
import com.fanhua.jobtrack.module.auth.dto.RegisterRequest;
import com.fanhua.jobtrack.module.auth.service.AuthService;
import com.fanhua.jobtrack.module.auth.vo.AuthTokenVO;
import com.fanhua.jobtrack.module.auth.vo.RegisterVO;
import com.fanhua.jobtrack.module.auth.vo.SessionVO;
import com.fanhua.jobtrack.module.auth.vo.UserVO;
import com.fanhua.jobtrack.security.LoginUser;
import com.fanhua.jobtrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 认证与会话接口。
 * Refresh Token 通过 HttpOnly Cookie 传递，前端 JS 无法读取；
 * Access Token 在响应体中返回，前端保存于内存。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证与会话", description = "注册、登录、令牌刷新、多设备会话管理")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @PostMapping("/register")
    @Operation(summary = "注册", description = "注册成功后需重新登录；用户名/邮箱冲突返回 409")
    public ResponseEntity<Result<RegisterVO>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterVO vo = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success("注册成功", vo));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "支持用户名或邮箱登录；连续失败将临时锁定")
    public ResponseEntity<Result<AuthTokenVO>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.login(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.plainRefreshToken(), result.refreshTtlSeconds()))
                .body(Result.success("登录成功", result.token()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "执行 Refresh Token Rotation，旧令牌立即失效")
    public ResponseEntity<Result<AuthTokenVO>> refresh(
            @CookieValue(name = "${jobtrack.auth.cookie-name:JOBTRACK_RT}", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        AuthService.RefreshResult result = authService.refresh(refreshToken, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.plainRefreshToken(), result.refreshTtlSeconds()))
                .body(Result.success("刷新成功", result.token()));
    }

    @PostMapping("/logout")
    @Operation(summary = "注销当前设备")
    public ResponseEntity<Result<Void>> logout() {
        LoginUser current = SecurityUtils.currentUser();
        authService.logout(current.getUserId(), current.getSessionId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
                .body(Result.success("已退出登录", null));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "注销全部设备", description = "keepCurrent=true 时保留当前设备")
    public ResponseEntity<Result<Void>> logoutAll(@RequestBody(required = false) Map<String, Boolean> body) {
        LoginUser current = SecurityUtils.currentUser();
        boolean keepCurrent = body != null && Boolean.TRUE.equals(body.get("keepCurrent"));
        authService.logoutAll(current.getUserId(), current.getSessionId(), keepCurrent);
        // 若连当前设备一并注销，同时清除 Cookie
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (!keepCurrent) {
            builder.header(HttpHeaders.SET_COOKIE, clearRefreshCookie());
        }
        return builder.body(Result.success("已注销指定会话", null));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser(SecurityUtils.currentUserId()));
    }

    @GetMapping("/sessions")
    @Operation(summary = "当前用户的有效会话列表")
    public Result<List<SessionVO>> sessions() {
        LoginUser current = SecurityUtils.currentUser();
        return Result.success(authService.listSessions(current.getUserId(), current.getSessionId()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "注销指定设备", description = "仅允许注销属于当前用户的会话")
    public Result<Void> revokeSession(@PathVariable String sessionId) {
        authService.revokeSession(SecurityUtils.currentUserId(), sessionId);
        return Result.success("已注销该设备", null);
    }

    /** 构造 Refresh Token Cookie：HttpOnly + SameSite=Strict + 限定认证路径 */
    private String buildRefreshCookie(String plainToken, long ttlSeconds) {
        return ResponseCookie.from(authProperties.getCookieName(), plainToken)
                .httpOnly(true)
                .secure(authProperties.isCookieSecure())
                .sameSite("Strict")
                .path(authProperties.getCookiePath())
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build()
                .toString();
    }

    private String clearRefreshCookie() {
        return ResponseCookie.from(authProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(authProperties.isCookieSecure())
                .sameSite("Strict")
                .path(authProperties.getCookiePath())
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }
}
