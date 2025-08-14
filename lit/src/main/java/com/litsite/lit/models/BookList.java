package com.litsite.lit.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "booklist")
@Getter @Setter @NoArgsConstructor
public class BookList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer listId;

    private String title;
    private LocalDateTime creationDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private MyUser user;

    @ManyToMany
    @JoinTable(
            name = "list_book",
            joinColumns = @JoinColumn(name = "list_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books = new HashSet<>();
}
