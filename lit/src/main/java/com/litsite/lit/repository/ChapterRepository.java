package com.litsite.lit.repository;

import com.litsite.lit.models.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findByBookBookIdOrderByChapterNumberAsc(Integer bookId);
}
