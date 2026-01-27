package com.project.byeoryback.domain.post.repository;

import com.project.byeoryback.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

        // 특정 유저의 글 목록 조회 (최신순)
        List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);

        // ✨ [New] 개인 기록만 조회 (그룹 활동 제외)
        List<Post> findAllByUserIdAndRoomIsNullOrderByCreatedAtDesc(Long userId);

        // [Optimization] 개인 기록 ID만 조회 (Sort Buffer Overflow 방지)
        @Query("SELECT p.id FROM Post p WHERE p.user.id = :userId AND p.room IS NULL ORDER BY p.createdAt DESC")
        List<Long> findIdsByUserIdAndRoomIsNullOrderByCreatedAtDesc(@Param("userId") Long userId);

        // 유저의 게시글 수 카운트
        long countByUserId(Long userId);

        // 특정 방의 글 목록 조회 (최신순)
        List<Post> findAllByRoomIdOrderByCreatedAtDesc(Long roomId);

        // 미분류 게시글 조회 (해시태그나 앨범에 포함되지 않은 글)
        @Query("SELECT p FROM Post p WHERE p.user.id = :userId " +
                        "AND p.id NOT IN (SELECT ph.post.id FROM PostHashtag ph) " +
                        "AND p.id NOT IN (SELECT ac.childPost.id FROM AlbumContent ac)")
        List<Post> findUnclassifiedPosts(@Param("userId") Long userId);

        // 유저 ID로 게시글 삭제
        void deleteByUserId(Long userId);

        // 모든 공개 게시글 페이징 조회
        Page<Post> findByIsPublicTrue(Pageable pageable);

        // 해시태그로 공개 게시글 검색
        @Query("SELECT p FROM Post p " +
                        "JOIN p.postHashtags ph " +
                        "JOIN ph.hashtag h " +
                        "WHERE p.isPublic = true AND h.name = :hashtag")
        Page<Post> findByIsPublicTrueAndHashtag(@Param("hashtag") String hashtag, Pageable pageable);

        // [Optimization] ID만 먼저 조회 (Sort Buffer Overflow 방지)
        @Query("SELECT p.id FROM Post p WHERE p.isPublic = true")
        Page<Long> findIdsByIsPublicTrue(Pageable pageable);

        @Query("SELECT p.id FROM Post p " +
                        "JOIN p.postHashtags ph " +
                        "JOIN ph.hashtag h " +
                        "WHERE p.isPublic = true AND h.name = :hashtag")
        Page<Long> findIdsByIsPublicTrueAndHashtag(@Param("hashtag") String hashtag, Pageable pageable);

        // 기간별(월별) 내 게시글 조회
        List<Post> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime start,
                        LocalDateTime end);

        // 특정 기간 내 게시글 존재 여부 확인
        boolean existsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

        // 특정 유저의 게시글 중 랜덤으로 1개 조회 (MySQL 기준)
        @Query(value = "SELECT * FROM post WHERE user_id = :userId ORDER BY RAND() LIMIT 1", nativeQuery = true)
        Post findRandomPostByUserId(@Param("userId") Long userId);
}