package com.litsite.lit.service;

import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Role;
import com.litsite.lit.repository.RoleRepository;
import com.litsite.lit.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<Role> getAllRoles() { return roleRepository.findAll(); }

    public MyUser getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public long countUsersWithRole(String roleName) {
        return userRepository.countByRolesName(roleName);
    }

    @Transactional
    public MyUser assignRole(Long userId, String roleName) {
        MyUser user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Роль не найдена"));
        user.addRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public MyUser removeRole(Long userId, String roleName) {
        MyUser user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (user.getRoles().size() <= 1 && "ROLE_USER".equals(roleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Должна остаться хотя бы одна роль");
        }
        user.removeRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public Role createRole(String roleName) {
        if (roleRepository.findByName(roleName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Роль уже существует");
        }
        return roleRepository.save(new Role(roleName));
    }
}