package com.litsite.lit.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "my_user")
@Getter
@Setter
@NoArgsConstructor
public class MyUser implements UserDetails {
    public MyUser(String username, String email, String encode, String login) {
        this.username = username;
        this.email = email;
        this.passwordHash = encode;
        this.login = login;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    private String login;
    private String passwordHash;
    private String username;
    private String email;
    private LocalDateTime registrationDate;
    private String profileDescription;
    private Boolean isEnabled;
    private String roles;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;

    public Boolean getEnabled() {
        return isEnabled;
    }

    public void setEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Book> books = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<BookList> bookLists = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Rating> ratings = new ArrayList<>();

    public MyUser(String login, String passwordHash, String username, String email, LocalDateTime registrationDate, String profileDescription) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.username = username;
        this.email = email;
        this.registrationDate = registrationDate;
        this.profileDescription = profileDescription;
    }

    public MyUser(String login, String passwordHash, String username, String email, LocalDateTime registrationDate, String profileDescription, List<Book> books, List<Comment> comments, List<BookList> bookLists, List<Rating> ratings) {
        this.login = login;
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

}

