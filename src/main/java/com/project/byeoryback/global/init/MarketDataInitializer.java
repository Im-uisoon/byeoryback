package com.project.byeoryback.global.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.byeoryback.domain.market.entity.MarketItem;
import com.project.byeoryback.domain.market.entity.MarketItemStatus;
import com.project.byeoryback.domain.market.repository.MarketItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MarketDataInitializer {

        private final MarketItemRepository marketItemRepository;
        private final ObjectMapper objectMapper;

        @Bean
        public CommandLineRunner initMarketData() {
                return args -> {
                        log.info("Initializing basic market data...");

                        List<Map<String, Object>> initialItems = Arrays.asList(
                                        Map.of(
                                                        "referenceId", "pack_basic",
                                                        "category", "package",
                                                        "name", "⭐ 스타터 팩",
                                                        "description", "다이어리 꾸미기의 기본! 필수 스티커 10종 모음.",
                                                        "price", 500L,
                                                        "tags", Arrays.asList("기본", "스타터", "필수"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/169/169367.png"),
                                        Map.of(
                                                        "referenceId", "sticker_pack_001",
                                                        "category", "package",
                                                        "name", "동물 스티커 팩",
                                                        "description", "다이어리에 쓰기 좋은 귀여운 강아지와 동물 친구들 스티커 모음입니다.",
                                                        "price", 800L,
                                                        "tags", Arrays.asList("귀여움", "강아지", "동물"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/616/616408.png"),
                                        Map.of(
                                                        "referenceId", "sticker_pack_002",
                                                        "category", "package",
                                                        "name", "빈티지 라벨",
                                                        "description", "감성적인 다꾸를 위한 빈티지 라벨 스티커입니다.",
                                                        "price", 700L,
                                                        "tags", Arrays.asList("빈티지", "라벨", "브라운"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2361/2361730.png"),
                                        // Removed dummy widget_template_001
                                        Map.of(
                                                        "referenceId", "post_template_001",
                                                        "category", "template_post",
                                                        "name", "공부 기록 템플릿",
                                                        "description", "오늘의 공부 시간을 기록하고 회고할 수 있는 템플릿입니다.",
                                                        "price", 800L,
                                                        "tags", Arrays.asList("공부", "다이어리", "템플릿"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3209/3209265.png"),
                                        Map.of(
                                                        "referenceId", "sticker_pack_003",
                                                        "category", "package",
                                                        "name", "픽셀 아트 이모지",
                                                        "description", "레트로 게임 감성의 픽셀 아트 이모지 팩!",
                                                        "price", 600L,
                                                        "tags", Arrays.asList("픽셀", "레트로", "이모지"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/10603/10603762.png"),
                                        Map.of(
                                                        "referenceId", "sticker_pack_004",
                                                        "category", "package",
                                                        "name", "여행 기록 팩",
                                                        "description", "여행의 설렘을 다이어리에! 비행기, 카메라 등 여행 아이템 모음.",
                                                        "price", 1100L,
                                                        "tags", Arrays.asList("여행", "추억", "기록"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/727/727289.png"),
                                        Map.of(
                                                        "referenceId", "sticker_pack_005",
                                                        "category", "package",
                                                        "name", "맛있는 음식 팩",
                                                        "description", "오늘 뭐 먹었지? 피자, 버거, 커피 등 맛있는 스티커들!",
                                                        "price", 800L,
                                                        "tags", Arrays.asList("음식", "맛집", "데일리"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3595/3595455.png"),
                                        Map.of(
                                                        "referenceId", "sticker_pack_006",
                                                        "category", "package",
                                                        "name", "일상 기록 팩",
                                                        "description", "소소한 일상의 순간들을 기록하기 좋은 데일리 아이템 모음.",
                                                        "price", 500L,
                                                        "tags", Arrays.asList("일상", "기록", "감성"),
                                                        "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2935/2935307.png"),
                                        // Individual Stickers (For Consolidated Purchase)
                                        Map.of("referenceId", "cat_1", "category", "sticker", "name", "동물 스티커 1",
                                                        "description", "귀여운 동물 스티커 (낱개)", "price", 200L, "tags",
                                                        Arrays.asList("동물", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/616/616408.png"),
                                        Map.of("referenceId", "cat_2", "category", "sticker", "name", "동물 스티커 2",
                                                        "description", "귀여운 동물 스티커 (낱개)", "price", 200L, "tags",
                                                        Arrays.asList("동물", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/616/616430.png"),
                                        Map.of("referenceId", "cat_3", "category", "sticker", "name", "동물 스티커 3",
                                                        "description", "귀여운 동물 스티커 (낱개)", "price", 200L, "tags",
                                                        Arrays.asList("동물", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2663/2663067.png"),
                                        Map.of("referenceId", "cat_4", "category", "sticker", "name", "동물 스티커 4",
                                                        "description", "귀여운 동물 스티커 (낱개)", "price", 200L, "tags",
                                                        Arrays.asList("동물", "낱개"), "imageUrl",
                                                        "https://img.icons8.com/color/512/panda.png"),
                                        Map.of("referenceId", "cat_5", "category", "sticker", "name", "동물 스티커 5",
                                                        "description", "귀여운 동물 스티커 (낱개)", "price", 200L, "tags",
                                                        Arrays.asList("동물", "낱개"), "imageUrl",
                                                        "https://img.icons8.com/color/512/fox.png"),
                                        Map.of("referenceId", "vintage_1", "category", "sticker", "name", "빈티지 라벨 1",
                                                        "description", "빈티지 라벨 스티커 (낱개)", "price", 300L, "tags",
                                                        Arrays.asList("빈티지", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2361/2361730.png"),
                                        Map.of("referenceId", "vintage_2", "category", "sticker", "name", "빈티지 라벨 2",
                                                        "description", "빈티지 라벨 스티커 (낱개)", "price", 300L, "tags",
                                                        Arrays.asList("빈티지", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3739/3739054.png"),
                                        Map.of("referenceId", "vintage_3", "category", "sticker", "name", "빈티지 라벨 3",
                                                        "description", "빈티지 라벨 스티커 (낱개)", "price", 300L, "tags",
                                                        Arrays.asList("빈티지", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2361/2361841.png"),
                                        Map.of("referenceId", "pixel_1", "category", "sticker", "name", "픽셀 이모지 1",
                                                        "description", "픽셀 아트 이모지 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("픽셀", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/10603/10603762.png"),
                                        Map.of("referenceId", "pixel_2", "category", "sticker", "name", "픽셀 이모지 2",
                                                        "description", "픽셀 아트 이모지 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("픽셀", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/10603/10603764.png"),
                                        Map.of("referenceId", "pixel_3", "category", "sticker", "name", "픽셀 이모지 3",
                                                        "description", "픽셀 아트 이모지 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("픽셀", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/10603/10603761.png"),
                                        Map.of("referenceId", "travel_1", "category", "sticker", "name", "여행 스티커 1",
                                                        "description", "여행 기록용 스티커 (낱개)", "price", 350L, "tags",
                                                        Arrays.asList("여행", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/727/727289.png"),
                                        Map.of("referenceId", "travel_2", "category", "sticker", "name", "여행 스티커 2",
                                                        "description", "여행 기록용 스티커 (낱개)", "price", 350L, "tags",
                                                        Arrays.asList("여행", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/685/685655.png"),
                                        Map.of("referenceId", "travel_3", "category", "sticker", "name", "여행 스티커 3",
                                                        "description", "여행 기록용 스티커 (낱개)", "price", 350L, "tags",
                                                        Arrays.asList("여행", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/1865/1865269.png"),
                                        Map.of("referenceId", "travel_4", "category", "sticker", "name", "여행 스티커 4",
                                                        "description", "여행 기록용 스티커 (낱개)", "price", 350L, "tags",
                                                        Arrays.asList("여행", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2903/2903554.png"),
                                        Map.of("referenceId", "food_1", "category", "sticker", "name", "음식 스티커 1",
                                                        "description", "맛있는 음식 스티커 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("음식", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3595/3595455.png"),
                                        Map.of("referenceId", "food_2", "category", "sticker", "name", "음식 스티커 2",
                                                        "description", "맛있는 음식 스티커 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("음식", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3075/3075977.png"),
                                        Map.of("referenceId", "food_3", "category", "sticker", "name", "음식 스티커 3",
                                                        "description", "맛있는 음식 스티커 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("음식", "낱개"), "imageUrl",
                                                        "https://img.icons8.com/color/512/hot-dog.png"),
                                        Map.of("referenceId", "food_4", "category", "sticker", "name", "음식 스티커 4",
                                                        "description", "맛있는 음식 스티커 (낱개)", "price", 250L, "tags",
                                                        Arrays.asList("음식", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3194/3194593.png"),
                                        Map.of("referenceId", "daily_1", "category", "sticker", "name", "데일리 스티커 1",
                                                        "description", "일상 오브제 스티커 (낱개)", "price", 150L, "tags",
                                                        Arrays.asList("일상", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2935/2935307.png"),
                                        Map.of("referenceId", "daily_2", "category", "sticker", "name", "데일리 스티커 2",
                                                        "description", "일상 오브제 스티커 (낱개)", "price", 150L, "tags",
                                                        Arrays.asList("일상", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3389/3389081.png"),
                                        Map.of("referenceId", "daily_3", "category", "sticker", "name", "데일리 스티커 3",
                                                        "description", "일상 오브제 스티커 (낱개)", "price", 150L, "tags",
                                                        Arrays.asList("일상", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/3659/3659899.png"),
                                        Map.of("referenceId", "daily_4", "category", "sticker", "name", "데일리 스티커 4",
                                                        "description", "일상 오브제 스티커 (낱개)", "price", 150L, "tags",
                                                        Arrays.asList("일상", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/2097/2097068.png"),
                                        Map.of("referenceId", "free_5", "category", "sticker", "name", "스마일",
                                                        "description", "기본 스마일 스티커", "price", 150L, "tags",
                                                        Arrays.asList("기본", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/742/742751.png"),
                                        Map.of("referenceId", "free_6", "category", "sticker", "name", "해",
                                                        "description", "기본 해 스티커", "price", 150L, "tags",
                                                        Arrays.asList("기본", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/169/169367.png"),
                                        Map.of("referenceId", "free_7", "category", "sticker", "name", "구름",
                                                        "description", "기본 구름 스티커", "price", 150L, "tags",
                                                        Arrays.asList("기본", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/414/414825.png"),
                                        Map.of("referenceId", "free_8", "category", "sticker", "name", "음표",
                                                        "description", "기본 음표 스티커", "price", 150L, "tags",
                                                        Arrays.asList("기본", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/651/651717.png"),
                                        Map.of("referenceId", "free_9", "category", "sticker", "name", "체크",
                                                        "description", "기본 체크 스티커", "price", 150L, "tags",
                                                        Arrays.asList("기본", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/190/190411.png"),
                                        Map.of("referenceId", "free_10", "category", "sticker", "name", "엄지척",
                                                        "description", "기본 엄지척 스티커", "price", 150L, "tags",
                                                        Arrays.asList("기본", "낱개"), "imageUrl",
                                                        "https://cdn-icons-png.flaticon.com/512/126/126473.png"));

                        for (Map<String, Object> data : initialItems) {
                                String referenceId = (String) data.get("referenceId");
                                String contentJson;

                                if ("post_template_001".equals(referenceId)) {
                                        // 공부 기록 템플릿용 JSON 생성 (PX 기반 레이아웃)
                                        contentJson = objectMapper.writeValueAsString(Map.of(
                                                        "name", data.get("name"),
                                                        "styles", Map.of(
                                                                        "backgroundColor", "#f8f9fa",
                                                                        "padding", "340px 3rem 3rem 3rem", // ✨ Secure
                                                                                                           // Header
                                                                                                           // Space
                                                                        "boxShadow",
                                                                        "0 4px 6px -1px rgba(0, 0, 0, 0.1)"),
                                                        "defaultFontColor", "#333333",
                                                        "stickers", List.of(),
                                                        "floatingTexts", Arrays.asList(
                                                                        Map.of(
                                                                                        "id", "ft-title", "x", "50%",
                                                                                        "y", "40px", "w", 300, "h", 50,
                                                                                        "zIndex", 10, "rotation", 0,
                                                                                        "text", "오늘의 공부", "styles",
                                                                                        Map.of("color", "#000000",
                                                                                                        "fontSize",
                                                                                                        "24px",
                                                                                                        "fontWeight",
                                                                                                        "bold",
                                                                                                        "textAlign",
                                                                                                        "center",
                                                                                                        "transform",
                                                                                                        "translateX(-50%)")),
                                                                        Map.of(
                                                                                        "id", "ft-subject", "x", "10%",
                                                                                        "y", "110px", "w", 300, "h", 40,
                                                                                        "zIndex", 10, "rotation", 0,
                                                                                        "text", "과목:",
                                                                                        "styles",
                                                                                        Map.of("color", "#555555",
                                                                                                        "fontSize",
                                                                                                        "16px")),
                                                                        Map.of(
                                                                                        "id", "ft-time", "x", "60%",
                                                                                        "y", "110px", "w", 200, "h", 40,
                                                                                        "zIndex", 10, "rotation", 0,
                                                                                        "text", "시간:",
                                                                                        "styles",
                                                                                        Map.of("color", "#555555",
                                                                                                        "fontSize",
                                                                                                        "16px")),
                                                                        Map.of(
                                                                                        "id", "ft-goal", "x", "10%",
                                                                                        "y", "170px", "w", 300, "h", 40,
                                                                                        "zIndex", 10, "rotation", 0,
                                                                                        "text", "🎯 오늘의 목표:", "styles",
                                                                                        Map.of("color", "#000000",
                                                                                                        "fontSize",
                                                                                                        "18px",
                                                                                                        "fontWeight",
                                                                                                        "bold")),
                                                                        Map.of(
                                                                                        "id", "ft-memo", "x", "10%",
                                                                                        "y", "220px", "w", 500, "h", 80,
                                                                                        "zIndex", 10, "rotation", 0,
                                                                                        "text", "",
                                                                                        "styles",
                                                                                        Map.of("color", "#888888",
                                                                                                        "fontSize",
                                                                                                        "14px",
                                                                                                        "fontStyle",
                                                                                                        "italic"))),
                                                        "floatingImages", List.of(),
                                                        "tags", data.get("tags"),
                                                        "thumbnailUrl", data.get("imageUrl")));
                                } else {
                                        contentJson = objectMapper.writeValueAsString(Map.of(
                                                        "description", data.get("description"),
                                                        "imageUrl", data.get("imageUrl"),
                                                        "tags", data.get("tags")));
                                }

                                MarketItem item;
                                if (marketItemRepository.existsByReferenceId(referenceId)) {
                                        item = marketItemRepository.findByReferenceId(referenceId).orElse(null);
                                } else {
                                        item = MarketItem.builder()
                                                        .name((String) data.get("name"))
                                                        .price((Long) data.get("price"))
                                                        .category((String) data.get("category"))
                                                        .description((String) data.get("description"))
                                                        .referenceId(referenceId)
                                                        .status(MarketItemStatus.ON_SALE)
                                                        .createdAt(LocalDateTime.now())
                                                        .build();
                                }

                                if (item != null) {
                                        item.setContentJson(contentJson);
                                        item.setName((String) data.get("name"));
                                        item.setCategory((String) data.get("category"));
                                        item.setPrice((Long) data.get("price"));
                                        item.setDescription((String) data.get("description"));
                                        marketItemRepository.save(item);
                                }
                        }
                        log.info("Basic market data initialization check completed.");
                };
        }
}
