package com.project.byeoryback.domain.post.service;

import com.project.byeoryback.domain.post.dto.PostRequest;
import com.project.byeoryback.domain.post.entity.Post;
import com.project.byeoryback.domain.post.repository.PostRepository;
import com.project.byeoryback.domain.user.entity.User;
import com.project.byeoryback.domain.hashtag.service.HashtagService;
import com.project.byeoryback.domain.album.service.AlbumService;
import com.project.byeoryback.domain.album.entity.AlbumContent;
import com.project.byeoryback.domain.album.entity.Album;
import com.project.byeoryback.domain.album.repository.AlbumRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final HashtagService hashtagService;
    private final AlbumService albumService;
    private final AlbumRepository albumRepository;
    // [추가됨] 커뮤니티 서비스 주입
    private final com.project.byeoryback.domain.community.service.CommunityService communityService;
    // [추가됨] 룸 리포지토리 주입
    private final com.project.byeoryback.domain.room.repository.RoomRepository roomRepository;

    // 1. 전체 조회 (최신순)
    public List<Post> getAllPosts() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // [New] 특정 유저의 게시글만 조회 (그룹 활동 제외, 순수 개인 기록만)
    public List<Post> getPostsByUserId(Long userId) {
        // [Optimization] 1. ID만 먼저 조회 (Index Scan, Lightweight)
        List<Long> ids = postRepository.findIdsByUserIdAndRoomIsNullOrderByCreatedAtDesc(userId);

        if (ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // [Optimization] 2. 실제 데이터 조회
        List<Post> posts = postRepository.findAllById(ids);

        // 3. 메모리 내 재정렬 (ID 리스트 순서에 맞춤)
        java.util.Map<Long, Post> postMap = posts.stream()
                .collect(java.util.stream.Collectors.toMap(Post::getId, java.util.function.Function.identity()));

        return ids.stream()
                .map(postMap::get)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    // 2. 게시글 생성
    @Transactional
    public Post createPost(User user, PostRequest request) {
        Post post = Post.builder()
                .title(request.getTitle())
                .titleStyles(request.getTitleStyles())
                .mode(request.getMode())
                .blocks(request.getBlocks())
                .stickers(request.getStickers())
                .floatingTexts(request.getFloatingTexts())
                .floatingImages(request.getFloatingImages())
                .user(user)
                .isFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .styles(request.getStyles())
                .build();

        // [Added] Room Association
        if (request.getRoomId() != null) {
            com.project.byeoryback.domain.room.entity.Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 방이 없습니다. id=" + request.getRoomId()));
            post.setRoom(room); // Post 엔티티에 setRoom 메서드(Setter)가 있어야 함 (Lombok @Data/@Setter 확인 필요)
        }

        Post savedPost = postRepository.save(post);

        // Hashtag processing
        hashtagService.processHashtags(savedPost, request.getTags());

        // Album assignment: Always trust Frontend's explicit IDs.
        // Frontend calculates proper album IDs for both MANUAL and AUTO modes.
        // Backend purely persists these associations without hidden auto-logic.
        if (request.getTargetAlbumIds() != null && !request.getTargetAlbumIds().isEmpty()) {
            for (Long albumId : request.getTargetAlbumIds()) {
                albumService.addContentToAlbum(albumId, savedPost.getId(), AlbumContent.ContentType.POST);
            }
        }
        // Folder assignment logic can be added similarly if needed

        return savedPost;
    }

    // 3. 게시글 수정
    @Transactional
    public Post updatePost(User user, Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        post.update(
                request.getTitle(),
                request.getTitleStyles(),
                request.getBlocks(),
                request.getStickers(),
                request.getFloatingTexts(),
                request.getFloatingImages(),
                request.getIsFavorite(),
                request.getMode(),
                request.getIsPublic(),
                request.getStyles());

        hashtagService.processHashtags(post, request.getTags());

        // [수정됨] 앨범 연결 관계 재설정 (Replace Logic)
        // 1. 기존의 모든 앨범 연결 제거
        post.getAlbumContents().clear();

        // 2. 새 앨범 목록 연결
        if (request.getTargetAlbumIds() != null && !request.getTargetAlbumIds().isEmpty()) {
            List<Album> targetAlbums = albumRepository.findAllById(request.getTargetAlbumIds());

            for (Album album : targetAlbums) {
                AlbumContent content = AlbumContent.builder()
                        .parentAlbum(album)
                        .childPost(post)
                        .contentType(AlbumContent.ContentType.POST)
                        .build();
                post.getAlbumContents().add(content);
            }
        }

        return post;
    }

    // 4. 삭제
    @Transactional
    public void deletePost(Long id) {
        communityService.deleteByPostId(id);
        postRepository.deleteById(id);
    }

    // 1년전 데이터 가져오기
    public Post getMemorablePostFromOneYearAgo(Long userId) { // 1년전 포스트 가져오기
        LocalDate today = LocalDate.now();
        LocalDate oneYearAgoDate = today.minusYears(1); // 기준점: 딱 1년 전 날짜

        // -------------------------------------------------------
        // 1단계: 딱 1년 전 '오늘' (00:00:00 ~ 23:59:59) 조회
        // -------------------------------------------------------
        LocalDateTime startOfDay = oneYearAgoDate.atStartOfDay();
        LocalDateTime endOfDay = oneYearAgoDate.atTime(LocalTime.MAX);

        List<Post> todayPosts = postRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                userId, startOfDay, endOfDay);

        if (!todayPosts.isEmpty()) {
            return getRichestPostByBlockCount(todayPosts);
        }

        // -------------------------------------------------------
        // 2단계: '오늘'이 없다면 -> [1년 전 그달] 부터 [3달 전]까지 역추적
        // -------------------------------------------------------
        // 예: 1년 전이 5월이면 -> 5월 전체 -> 4월 전체 -> 3월 전체 순으로 조회
        for (int i = 0; i < 3; i++) {
            // 검사할 월 계산 (0: 이번달, 1: 전달, 2: 전전달)
            YearMonth targetYearMonth = YearMonth.from(oneYearAgoDate.minusMonths(i));

            // 그 달의 1일 00:00:00 ~ 그 달의 말일 23:59:59
            LocalDateTime startOfMonth = targetYearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = targetYearMonth.atEndOfMonth().atTime(LocalTime.MAX);

            // ** 기존 리포지토리 메서드 재활용 **
            List<Post> monthlyPosts = postRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    userId, startOfMonth, endOfMonth);

            if (!monthlyPosts.isEmpty()) {
                return getRichestPostByBlockCount(monthlyPosts);
            }
        }

        // 3단계: 3달을 뒤져도 없으면 메시지 처리를 위한 예외 발생
        throw new IllegalStateException("추억을 불러올 데이터가 없어요.");
    }

    public Post findRandomPostByUserId(Long userId) {
        return postRepository.findRandomPostByUserId(userId);

    }

    // 5. 월별 게시글 요약 조회
    public List<com.project.byeoryback.domain.post.dto.PostSummaryResponse> getPostsSummaryByYearMonth(Long userId,
            int year, int month) {
        java.time.LocalDateTime start = java.time.LocalDateTime.of(year, month, 1, 0, 0, 0);
        java.time.LocalDateTime end = start.plusMonths(1).minusNanos(1); // End of the month

        List<Post> posts = postRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end);
        return posts.stream()
                .map(com.project.byeoryback.domain.post.dto.PostSummaryResponse::from)
                .toList();
    }

    // 블록(Block) 개수가 가장 많은 포스트 반환
    private Post getRichestPostByBlockCount(List<Post> posts) {
        return posts.stream()
                // blocks가 null일 경우 0으로 처리하여 에러 방지
                .max(Comparator.comparingInt(post -> {
                    if (post.getBlocks() == null)
                        return 0;
                    return post.getBlocks().size();
                }))
                .orElse(posts.get(0));
    }
}