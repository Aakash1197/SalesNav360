package com.salesnav360.service;

import com.salesnav360.dto.PageResponse;
import com.salesnav360.dto.RoleCreateRequest;
import com.salesnav360.dto.UserCreateRequest;
import com.salesnav360.dto.UserResponse;
import com.salesnav360.dto.UserUpdateRequest;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {

    PageResponse<UserResponse> searchUsers(String query, Pageable pageable);

    UserResponse createUser(UserCreateRequest request, String actor);

    UserResponse updateUser(Long id, UserUpdateRequest request, String actor);

    List<String> listRoles();

    String createRole(RoleCreateRequest request, String actor);
}

