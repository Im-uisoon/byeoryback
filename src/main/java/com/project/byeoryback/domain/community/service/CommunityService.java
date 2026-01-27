package com.project.byeoryback.domain.community.service;

import com.project.byeoryback.domain.community.dto.CommunityDto;
import com.project.byeoryback.domain.post.entity.Post;
import com.project.byeoryback.domain.post.entity.PostLike;
import com.project.byeoryback.domain.post.entity.PostStat;
import com.project.byeoryback.domain.post.repository.PostLikeRepository;
import com.project.byeoryback.domain.post.repository.PostRepository;
import com.project.byeoryback.domain.post.repository.PostStatRepository;
import com.project.byeoryback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostRepository postRepository;
    private final PostStatRepository postStatRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    /**
     * 커뮤니티 글 상세 조회 (조회수 증가 포함)
     * + 로그인한 유저의 좋아요 여부 체크
     */
    @Transactional
    public CommunityDto.Response getCommunity(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId));

        // 공개글이 아니면 접근 불가 처리 (선택사항)
        if (!Boolean.TRUE.equals(post.getIsPublic())) {
            throw new IllegalArgumentException("비공개 게시글입니다.");
        }

        // PostStat 조회 없으면 생성
        PostStat postStat = postStatRepository.findByPostId(postId).orElseGet(() -> {
            PostStat newStat = PostStat.builder().post(post).build();
            return postStatRepository.save(newStat);
        });

        postStat.increaseViewCount(); // 조회수 증가 (Dirty Checking)

        boolean isLiked = false;
        if (userId != null && userId != 0L) {
            com.project.byeoryback.domain.user.entity.User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다. id=" + userId));
            isLiked = postLikeRepository.existsByUserAndPost(user, post);
        }

        // DTO 변환 시 Post와 PostStat 정보 합침
        return CommunityDto.Response.from(post, postStat, isLiked);
    }

    @Transactional // 조회만 하므로 readOnly 권장
    public CommunityDto.Response getCommunityForCard(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId));

        // 공개글 체크
        if (!Boolean.TRUE.equals(post.getIsPublic())) {
            throw new IllegalArgumentException("비공개 게시글입니다.");
        }

        // PostStat 조회 (조회수 증가는 하지 않음)
        // 만약 통계 데이터가 아예 없다면 0으로 생성해서 보여줌
        PostStat postStat = postStatRepository.findByPostId(postId).orElseGet(() -> {
            PostStat newStat = PostStat.builder().post(post).build();
            return postStatRepository.save(newStat);
        });

        // 좋아요 여부 체크
        boolean isLiked = false;
        if (userId != null && userId != 0L) {
            // exists 쿼리가 가볍기 때문에 유저 존재 여부 체크 없이 바로 날려도 무방하지만,
            // 안전하게 하려면 유저 조회 후 진행
            try {
                com.project.byeoryback.domain.user.entity.User user = userRepository.getReferenceById(userId);
                isLiked = postLikeRepository.existsByUserAndPost(user, post);
            } catch (Exception e) {
                // 유저 ID가 잘못되었거나 없는 경우 false 처리
            }
        }

        return CommunityDto.Response.from(post, postStat, isLiked);
    }

    @Transactional
    public void increaseViewCount(Long postId) {
        PostStat postStat = postStatRepository.findByPostId(postId)
                .orElseGet(() -> {
                    Post post = postRepository.findById(postId)
                            .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다."));
                    return postStatRepository.save(PostStat.builder().post(post).build());
                });

        postStat.increaseViewCount(); // Dirty Checking으로 자동 업데이트
    }

    // 단순 조회용 (오버로딩)
    @Transactional
    public CommunityDto.Response getCommunity(Long postId) {
        return getCommunity(postId, 0L);
    }

    /**
     * 커뮤니티 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CommunityDto.Response> getCommunityList(
            org.springframework.data.domain.Pageable pageable, Long userId, String hashtag) {

        // [Optimization] 1. ID만 먼저 조회 (Index Scan, Lightweight)
        org.springframework.data.domain.Page<Long> idPage;
        if (hashtag != null && !hashtag.isBlank()) {
            idPage = postRepository.findIdsByIsPublicTrueAndHashtag(hashtag, pageable);
        } else {
            idPage = postRepository.findIdsByIsPublicTrue(pageable);
        }

        // 해당 페이지에 데이터가 없으면 빈 결과 반환
        if (idPage.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }

        java.util.List<Long> ids = idPage.getContent();

        // [Optimization] 2. 실제 데이터 조회 (WHERE IN id)
        // 주의: findAllById는 순서를 보장하지 않으므로, 아래에서 ID 순서대로 재정렬해야 함
        java.util.List<Post> posts = postRepository.findAllById(ids);

        // 3. 메모리 내 재정렬 (ID 리스트 순서에 맞춤)
        java.util.Map<Long, Post> postMap = posts.stream()
                .collect(java.util.stream.Collectors.toMap(Post::getId, java.util.function.Function.identity()));

        java.util.List<Post> sortedPosts = ids.stream()
                .map(postMap::get)
                .filter(java.util.Objects::nonNull) // 혹시 모를 null 방지
                .collect(java.util.stream.Collectors.toList());

        // 4. PageImpl로 감싸서 반환 (Page 정보는 idPage의 것을 그대로 사용)
        return new org.springframework.data.domain.PageImpl<>(sortedPosts, pageable, idPage.getTotalElements())
                .map(post -> {
                    PostStat postStat = postStatRepository.findByPostId(post.getId())
                            .orElse(PostStat.builder().post(post).build()); // 없으면 기본값(0)

                    boolean isLiked = false;
                    if (userId != null && userId != 0L) {
                        try {
                            com.project.byeoryback.domain.user.entity.User user = userRepository
                                    .getReferenceById(userId);
                            isLiked = postLikeRepository.existsByUserAndPost(user, post);
                        } catch (Exception e) {
                            // 유저 없을 경우 무시
                        }
                    }
                    return CommunityDto.Response.from(post, postStat, isLiked);
                });
    }

    /**
     * 좋아요 토글
     */
    @Transactional
    public void toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId));

        com.project.byeoryback.domain.user.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다. id=" + userId));

        // PostStat 조회 혹은 생성
        PostStat postStat = postStatRepository.findByPostId(postId).orElseGet(() -> {
            return postStatRepository.save(PostStat.builder().post(post).build());
        });

        boolean exists = postLikeRepository.existsByUserAndPost(user, post);

        if (exists) {
            // 이미 좋아요 누름 -> 취소
            postLikeRepository.deleteByUserAndPost(user, post);
            postStat.decreaseLikeCount();
        } else {
            // 좋아요 안 누름 -> 추가
            PostLike like = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            postLikeRepository.save(like);
            postStat.increaseLikeCount();
        }
    }

    /**
     * 게시글이 삭제될 때 통계 데이터도 정리 (PostService에서 호출 권장)
     */
    @Transactional
    public void deleteByPostId(Long postId) {
        postStatRepository.deleteByPostId(postId);
    }
}