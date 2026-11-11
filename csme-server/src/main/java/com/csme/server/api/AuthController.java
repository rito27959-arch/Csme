package com.csme.server.api;

import com.csme.server.application.AuthService;
import com.csme.server.infrastructure.security.AuthenticatedOnly;
import com.csme.server.infrastructure.security.PublicEndpoint;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.AuthDtos.ChangePasswordRequest;
import com.csme.shared.dto.AuthDtos.CurrentUser;
import com.csme.shared.dto.AuthDtos.LoginRequest;
import com.csme.shared.dto.AuthDtos.RefreshRequest;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PublicEndpoint
    @PostMapping(ApiRoutes.AUTH_LOGIN)
    public TokenResponse login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PublicEndpoint
    @PostMapping(ApiRoutes.AUTH_REFRESH)
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        return auth.refresh(request == null ? null : request.refreshToken());
    }

    @PublicEndpoint
    @PostMapping(ApiRoutes.AUTH_LOGOUT)
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        auth.logout(request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @AuthenticatedOnly
    @GetMapping(ApiRoutes.AUTH_ME)
    public CurrentUser me() {
        return auth.me();
    }

    @AuthenticatedOnly
    @PostMapping(ApiRoutes.AUTH_CHANGE_PASSWORD)
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        auth.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
