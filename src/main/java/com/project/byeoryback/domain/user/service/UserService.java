package com.project.byeoryback.domain.user.service;

import com.project.byeoryback.domain.user.dto.UserProfileRequest;
import com.project.byeoryback.domain.user.dto.UserProfileResponse;
import com.project.byeoryback.domain.user.entity.User;
import com.project.byeoryback.domain.user.entity.UserProfile;
import com.project.byeoryback.domain.user.repository.UserProfileRepository;
import com.project.byeoryback.domain.user.repository.UserRepository;
import com.project.byeoryback.domain.user.exception.UserProfileNotFoundException;

import com.project.byeoryback.domain.user.exception.NicknameAlreadyExistsException;
import com.project.byeoryback.domain.post.repository.PostRepository;
import com.project.byeoryback.domain.setting.page.repository.PageSettingRepository;
import com.project.byeoryback.domain.setting.theme.repository.ThemeSettingRepository;
import com.project.byeoryback.domain.setting.menu.repository.MenuSettingRepository;
import com.project.byeoryback.domain.album.repository.AlbumRepository;
import com.project.byeoryback.domain.room.repository.RoomRepository;
import com.project.byeoryback.domain.room.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.byeoryback.domain.post.repository.PostTemplateRepository;

import com.project.byeoryback.domain.quest.repository.QuestLogRepository;

@Service
@RequiredArgsConstructor
public class UserService {

        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final PostRepository postRepository;
        private final PostTemplateRepository postTemplateRepository; // Added for deletion
        private final com.project.byeoryback.domain.post.repository.PostStatRepository postStatRepository; // Added

        private final PageSettingRepository pageSettingRepository;
        private final ThemeSettingRepository themeSettingRepository;
        private final MenuSettingRepository menuSettingRepository;
        private final com.project.byeoryback.domain.setting.widget.repository.WidgetSettingRepository widgetSettingRepository;
        private final com.project.byeoryback.domain.setting.widget.repository.WidgetPresetRepository widgetPresetRepository;
        private final com.project.byeoryback.domain.setting.header.repository.HeaderSettingRepository headerSettingRepository;
        private final QuestLogRepository questLogRepository;

        private final AlbumRepository albumRepository;
        private final RoomRepository roomRepository;
        private final RoomMemberRepository roomMemberRepository;

        @Transactional(readOnly = true)
        public UserProfileResponse getUserProfile(Long userId) {
                UserProfile profile = userProfileRepository.findByUserId(userId)
                                .orElseThrow(() -> new UserProfileNotFoundException(userId));

                // Stats Calculation
                // 1. Total Entries
                long totalEntries = postRepository.countByUserId(userId);

                // 2. Received Likes
                Long receivedLikes = postStatRepository.sumLikesByUserId(userId);
                if (receivedLikes == null)
                        receivedLikes = 0L;

                // 3. Streak Days
                long streakDays = 0;
                java.util.List<com.project.byeoryback.domain.post.entity.Post> posts = postRepository
                                .findAllByUserIdOrderByCreatedAtDesc(userId);

                if (!posts.isEmpty()) {
                        java.time.LocalDate today = java.time.LocalDate.now();
                        java.time.LocalDate yesterday = today.minusDays(1);
                        java.time.LocalDate lastDate = posts.get(0).getCreatedAt().toLocalDate();

                        // 연속일 계산은 오늘 혹은 어제 작성한 글이 있어야 시작됨 (그렇지 않으면 0일 or 끊김)
                        // 하지만 "현재 연속 며칠째인가"를 보여주려면,
                        // 가장 최근 글이 오늘이면 -> 오늘부터 카운트
                        // 가장 최근 글이 어제이면 -> 어제부터 카운트
                        // 그 이전이면 -> 0일 (연속 끊김)

                        if (lastDate.equals(today) || lastDate.equals(yesterday)) {
                                streakDays = 1;
                                java.time.LocalDate currentDate = lastDate;

                                for (int i = 1; i < posts.size(); i++) {
                                        java.time.LocalDate nextDate = posts.get(i).getCreatedAt().toLocalDate();
                                        if (nextDate.equals(currentDate)) {
                                                continue; // 같은 날 여러 글 작성 시 패스
                                        }
                                        if (nextDate.equals(currentDate.minusDays(1))) {
                                                streakDays++;
                                                currentDate = nextDate;
                                        } else {
                                                break; // 연속 끊김
                                        }
                                }
                        }
                }

                return new UserProfileResponse(
                                profile.getUser().getId(),
                                profile.getProfilePhoto(),
                                profile.getName(),
                                profile.getNickname(),
                                profile.getUser().getEmail(),
                                profile.getBirthDate(),
                                profile.getPhone(),
                                profile.getGender(),
                                profile.getBio(),
                                profile.getUser().getCredits(),
                                totalEntries,
                                streakDays,
                                receivedLikes);
        }

        @Transactional
        public void updateUserProfile(Long userId, UserProfileRequest request) {
                UserProfile profile = userProfileRepository.findByUserId(userId)
                                .orElseThrow(() -> new UserProfileNotFoundException(userId));

                if (!profile.getNickname().equals(request.nickname())
                                && userProfileRepository.existsByNickname(request.nickname())) {
                        throw new NicknameAlreadyExistsException("Nickname already exists: " + request.nickname());
                }

                profile.update(
                                request.profilePhoto(),
                                request.name(),
                                request.nickname(),
                                request.birthDate(),
                                request.phone(),
                                request.gender(),
                                request.bio());
        }

        @Transactional
        public void completeUserProfile(Long userId, UserProfileRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                if (user.isFullProfile()) {
                        throw new IllegalStateException("User profile is already completed");
                }

                if (userProfileRepository.existsByNickname(request.nickname())) {
                        throw new NicknameAlreadyExistsException("Nickname already exists: " + request.nickname());
                }

                UserProfile userProfile = UserProfile.builder()
                                .user(user)
                                .profilePhoto(request.profilePhoto())
                                .name(request.name())
                                .nickname(request.nickname())
                                .birthDate(request.birthDate())
                                .phone(request.phone())
                                .gender(request.gender())
                                .bio(request.bio())
                                .build();

                userProfileRepository.save(userProfile);

                user.completeProfile();
                // Transactional will dirty check and save user update, but explicitly saving if
                // needed
                // userRepository.save(user);
        }

        @Transactional
        public void deleteUser(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                // 1. Delete User Profile
                // Handled by CascadeType.ALL on User entity
                // userProfileRepository.findByUserId(userId).ifPresent(userProfileRepository::delete);

                // 2. Delete Posts
                // Use deleteAll(entities) to trigger JPA cascades (Post -> PostStat, PostLike,
                // etc.)
                // deleteByUserId (raw delete) causes Foreign Key Constraint violations.
                java.util.List<com.project.byeoryback.domain.post.entity.Post> posts = postRepository
                                .findAllByUserIdOrderByCreatedAtDesc(userId);
                posts.forEach(post -> {
                        // Post와 연관된 엔티티들이 Cascade 설정이 안되어 있을 수 있으므로 확인 필요
                        postRepository.delete(post);
                });

                // 2-1. Delete Post Templates
                postTemplateRepository.deleteAll(postTemplateRepository.findAllByUser(user));

                // 3. Delete Todos
                // Handled by CascadeType.ALL on User entity

                // 3-1. Delete Quest Logs
                questLogRepository.deleteByUser(user);

                // 4. Delete Settings
                pageSettingRepository.deleteByUserId(userId);
                themeSettingRepository.deleteByUserId(userId);
                menuSettingRepository.deleteByUserId(userId);
                widgetSettingRepository.deleteByUser(user);
                widgetPresetRepository.deleteByUser(user); // Delete widget presets
                headerSettingRepository.deleteByUser(user);

                // 5. Delete Albums and Folders
                albumRepository.deleteAll(albumRepository.findAllByUserId(userId));

                // 6. Delete Rooms (Owner) and Memberships
                // Policy: Delete rooms owned by the user (Cascades to members and posts within
                // the room)
                roomRepository.deleteAll(roomRepository.findAllByOwnerId(userId));
                // Remove user from other rooms they are a member of
                roomMemberRepository.deleteAll(roomMemberRepository.findAllByUserId(userId));

                // 7. Delete Market Data (Purchase History, Wishlist, Reviews)
                // MarketTransaction and Review and Wishlist are handled by CascadeType.ALL on
                // User entity

                // 8. Delete User
                userRepository.delete(user);
        }

        @Transactional
        public void recordHeartbeat(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                user.recordHeartbeat(java.time.LocalDate.now(), 60L);
        }

        @Transactional(readOnly = true)
        public Long getPlayTime(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                // If the last play date is not today, the play time is effectively 0 for today
                // (Though we might want to return the stored time if we want to show
                // "yesterday's" time before first heartbeat?
                // But the requirement says "current access time". If it's a new day, it should
                // resume from 0 or start accumulating)
                // However, without a heartbeat, the DB might still show old time.
                // Let's rely on the client to start heartbeating.
                // But for display purposes, if date is old, return 0.
                if (user.getLastPlayDate() == null || !user.getLastPlayDate().equals(java.time.LocalDate.now())) {
                        return 0L;
                }
                return user.getTodayPlayTime();
        }

        @Transactional
        public void addCredits(Long userId, Long amount) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                user.addCredits(amount);
        }

        @Transactional(readOnly = true)
        public User getUserByEmail(String email) {
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        }
}
