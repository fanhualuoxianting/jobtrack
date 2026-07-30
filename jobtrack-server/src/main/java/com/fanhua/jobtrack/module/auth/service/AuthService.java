package com.fanhua.jobtrack.module.auth.service;

import com.fanhua.jobtrack.module.auth.dto.LoginRequest;
import com.fanhua.jobtrack.module.auth.dto.RegisterRequest;
import com.fanhua.jobtrack.module.auth.vo.AuthTokenVO;
import com.fanhua.jobtrack.module.auth.vo.RegisterVO;
import com.fanhua.jobtrack.module.auth.vo.SessionVO;
import com.fanhua.jobtrack.module.auth.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 认证服务：注册、登录、令牌刷新与多设备会话管理
 */
public interface AuthService {

    /** 注册新用户并创建默认设置；用户名/邮箱冲突返回 409 */
    RegisterVO register(RegisterRequest request);

    /**
     * 登录。
     * 返回令牌信息和需要写入 Cookie 的明文 Refresh Token；
     * 失败计数、锁定、审计在本方法内完成。
     */
    LoginResult login(LoginRequest request, HttpServletRequest httpRequest);

    /** 用 Cookie 中的 Refresh Token 轮换并签发新令牌 */
    RefreshResult refresh(String plainRefreshToken, HttpServletRequest httpRequest);

    /** 注销当前会话 */
    void logout(Long userId, String sessionId);

    /** 注销当前用户会话；keepCurrent=true 时保留发起请求的当前设备 */
    void logoutAll(Long userId, String currentSessionId, boolean keepCurrent);

    /** 当前用户信息 */
    UserVO getCurrentUser(Long userId);

    /** 当前用户有效会话列表 */
    List<SessionVO> listSessions(Long userId, String currentSessionId);

    /** 注销指定会话，仅允许操作自己的会话 */
    void revokeSession(Long userId, String sessionId);

    /** 登录结果：令牌 + 需写入 Cookie 的 Refresh Token */
    record LoginResult(AuthTokenVO token, String plainRefreshToken, long refreshTtlSeconds) {
    }

    /** 刷新结果：新 Access Token + 新 Refresh Token */
    record RefreshResult(AuthTokenVO token, String plainRefreshToken, long refreshTtlSeconds) {
    }
}
