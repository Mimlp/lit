package com.litsite.lit.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "my_user")
@Data
@ToString
@NoArgsConstructor
public class MyUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String passwordHash;
    private String username;
    private String email;
    private LocalDateTime registrationDate;
    private String profileDescription;
    private Boolean isEnabled;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    public Boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.EAGER) // EAGER важен для Spring Security
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Book> books = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<BookList> bookLists = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Rating> ratings = new ArrayList<>();

    public MyUser(String passwordHash, String username, String email, LocalDateTime registrationDate, String profileDescription) {
        this.passwordHash = passwordHash;
        this.username = username;
        this.email = email;
        this.registrationDate = registrationDate;
        this.profileDescription = profileDescription;
    }

    public MyUser(String passwordHash, String username, String email, LocalDateTime registrationDate, String profileDescription, List<Book> books, List<Comment> comments, List<BookList> bookLists, List<Rating> ratings) {
        this.passwordHash = passwordHash;
        this.username = username;
        this.email = email;
        this.registrationDate = registrationDate;
        this.profileDescription = profileDescription;
        this.books = books;
        this.comments = comments;
        this.bookLists = bookLists;
        this.ratings = ratings;
    }
    public MyUser(String passwordHash, String username, String email,
                  LocalDateTime registrationDate, String profileDescription,
                  Set<Role> roles) {
        this.passwordHash = passwordHash;
        this.username = username;
        this.email = email;
        this.registrationDate = registrationDate;
        this.profileDescription = profileDescription;
        this.roles = roles != null ? roles : Set.of();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> (GrantedAuthority) () -> role.getName())
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    public boolean hasAnyRole(String... roleNames) {
        return roles.stream().anyMatch(r -> Arrays.asList(roleNames).contains(r.getName()));
    }

    public boolean isAdminOrModerator() {
        return hasAnyRole("ROLE_ADMIN", "ROLE_MODERATOR");
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }
}

