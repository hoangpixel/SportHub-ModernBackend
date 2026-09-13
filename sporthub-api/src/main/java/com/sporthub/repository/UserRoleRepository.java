package com.sporthub.repository;

import com.sporthub.entity.UserRole;
import com.sporthub.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(Long userId);

    boolean existsByUser_IdAndRole_Id(
            Long userId,
            Long roleId
    );
        void deleteByUser_Id(Long userId);
}