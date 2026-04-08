package com.litsite.lit.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chapter")
@Getter @Setter @NoArgsConstructor
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chapterId;

    private Integer chapterNumber;
    private String chapterText;
    private String chapterTitle;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    public void decreaseChapterNumber() {
        this.chapterNumber--;
    }
    public void increaseChapterNumber() {
        this.chapterNumber++;
    }
}
