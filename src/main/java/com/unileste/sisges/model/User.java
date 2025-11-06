package com.unileste.sisges.model;

import com.unileste.sisges.constants.Constants;
import com.unileste.sisges.enums.UserRoleENUM;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", schema = "sisges")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, unique = true)
    private String register;
    @Column(nullable = false, unique = true)
    private String password;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(nullable = false)
    private String gender;
    @Column(name = "user_role")
    @Enumerated(EnumType.ORDINAL)
    private UserRoleENUM userRole;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.userRole == UserRoleENUM.DEV_ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority(Constants.ROLE_DEV_ADMIN),
                    new SimpleGrantedAuthority(Constants.ROLE_ADMIN),
                    new SimpleGrantedAuthority(Constants.ROLE_STUDENT),
                    new SimpleGrantedAuthority(Constants.ROLE_TEACHER)
            );
        } else if (this.userRole == UserRoleENUM.ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority(Constants.ROLE_ADMIN),
                    new SimpleGrantedAuthority(Constants.ROLE_STUDENT)
            );
        } else if (this.userRole == UserRoleENUM.TEACHER) {
            return List.of(
                    new SimpleGrantedAuthority(Constants.ROLE_TEACHER)
            );
        }

        return List.of(
                new SimpleGrantedAuthority(Constants.ROLE_STUDENT)
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}