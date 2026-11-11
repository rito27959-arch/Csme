package com.csme.server.api;

import com.csme.server.application.RoleService;
import com.csme.server.application.UserService;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.CompanyDtos.SetActiveRequest;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.ResetPasswordRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.UpdateUserRequest;
import com.csme.shared.dto.UserDtos.UserDto;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService users;
    private final RoleService roles;

    public UserController(UserService users, RoleService roles) {
        this.users = users;
        this.roles = roles;
    }

    @RequiresPermission(module = ErpModule.USERS, action = Action.VIEW)
    @GetMapping(ApiRoutes.USERS)
    public PageResponse<UserDto> search(@RequestParam(name = "q", required = false) String query,
                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                        @RequestParam(name = "size", defaultValue = "25") int size) {
        return users.search(query, page, size);
    }

    @RequiresPermission(module = ErpModule.USERS, action = Action.CREATE)
    @PostMapping(ApiRoutes.USERS)
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody CreateUserRequest request) {
        return users.create(request);
    }

    @RequiresPermission(module = ErpModule.USERS, action = Action.UPDATE)
    @PutMapping(ApiRoutes.USERS + "/{id}")
    public UserDto update(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return users.update(id, request);
    }

    @RequiresPermission(module = ErpModule.USERS, action = Action.UPDATE)
    @PatchMapping(ApiRoutes.USERS + "/{id}/active")
    public UserDto setActive(@PathVariable UUID id, @RequestBody SetActiveRequest request) {
        return users.setActive(id, request != null && request.active());
    }

    @RequiresPermission(module = ErpModule.USERS, action = Action.UPDATE)
    @PostMapping(ApiRoutes.USERS + "/{id}/unlock")
    public UserDto unlock(@PathVariable UUID id) {
        return users.unlock(id);
    }

    @RequiresPermission(module = ErpModule.USERS, action = Action.UPDATE)
    @PostMapping(ApiRoutes.USERS + "/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @RequestBody ResetPasswordRequest request) {
        users.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @RequiresPermission(module = ErpModule.ROLES, action = Action.VIEW)
    @GetMapping(ApiRoutes.ROLES)
    public List<RoleDto> roles() {
        return roles.list();
    }
}
