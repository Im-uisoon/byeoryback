package com.project.byeoryback.domain.post.service;

import com.project.byeoryback.domain.post.dto.PostTemplateDto;
import com.project.byeoryback.domain.post.entity.PostTemplate;
import com.project.byeoryback.domain.post.repository.PostTemplateRepository;
import com.project.byeoryback.domain.user.entity.User;
import com.project.byeoryback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostTemplateService {

    private final PostTemplateRepository postTemplateRepository;
    private final UserRepository userRepository;
    private final com.project.byeoryback.domain.market.repository.MarketItemRepository marketItemRepository;
    private final com.project.byeoryback.domain.market.repository.MarketTransactionRepository marketTransactionRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public List<PostTemplateDto.Response> getMyTemplates(Long userId, Boolean showHidden) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<PostTemplate> templates = postTemplateRepository.findAllByUserOrderByCreatedAtDesc(user);

        // ✨ 1. Sync Missing Purchases (Retroactive Fix)
        try {
            List<com.project.byeoryback.domain.market.entity.MarketTransaction> transactions = marketTransactionRepository
                    .findByBuyerIdOrderByTransactionDateDesc(userId);
            boolean addedNew = false;

            for (com.project.byeoryback.domain.market.entity.MarketTransaction tx : transactions) {
                com.project.byeoryback.domain.market.entity.MarketItem item = tx.getItem();
                if ("template_post".equalsIgnoreCase(item.getCategory())
                        || "TEMPLATE".equalsIgnoreCase(item.getCategory())) {

                    // Check for Exact ID Match
                    PostTemplate existing = templates.stream()
                            .filter(t -> t.getSourceMarketItemId() != null
                                    && t.getSourceMarketItemId().equals(item.getId()))
                            .findFirst().orElse(null);

                    // Check for Name Match (Legacy backfill)
                    if (existing == null) {
                        existing = templates.stream()
                                .filter(t -> t.getSourceMarketItemId() == null
                                        && t.getName().equals(item.getName())
                                        && t.getTags() != null && t.getTags().contains("acquired"))
                                .findFirst().orElse(null);

                        if (existing != null) {
                            // Backfill ID
                            existing.setSourceMarketItemId(item.getId());
                            // Also refresh content to ensure it's up to date
                            if (item.getContentJson() != null) {
                                try {
                                    PostTemplateDto.CreateRequest freshData = objectMapper
                                            .readValue(item.getContentJson(), PostTemplateDto.CreateRequest.class);
                                    existing.update(freshData.getName(), freshData.getStyles(), freshData.getStickers(),
                                            existing.getFloatingTexts(), existing.getFloatingImages(),
                                            freshData.getDefaultFontColor(), freshData.getThumbnailUrl());
                                } catch (Exception e) {
                                }
                            }
                            postTemplateRepository.save(existing);
                        }
                    }

                    if (existing == null) {
                        // Create missing template
                        if (item.getContentJson() != null) {
                            try {
                                PostTemplateDto.CreateRequest freshData = objectMapper.readValue(item.getContentJson(),
                                        PostTemplateDto.CreateRequest.class);
                                PostTemplate newTemplate = PostTemplate.builder()
                                        .user(user)
                                        .name(item.getName())
                                        .styles(freshData.getStyles())
                                        .stickers(freshData.getStickers())
                                        .floatingTexts(freshData.getFloatingTexts())
                                        .floatingImages(freshData.getFloatingImages())
                                        .defaultFontColor(freshData.getDefaultFontColor())
                                        .thumbnailUrl(freshData.getThumbnailUrl())
                                        .sourceMarketItemId(item.getId())
                                        .tags(java.util.stream.Stream.concat(
                                                freshData.getTags() != null ? freshData.getTags().stream()
                                                        : java.util.stream.Stream.empty(),
                                                java.util.stream.Stream.of("acquired"))
                                                .collect(java.util.stream.Collectors.toList()))
                                        .build();
                                postTemplateRepository.save(newTemplate);
                                addedNew = true;
                            } catch (Exception e) {
                                System.err.println("Failed to sync template: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            if (addedNew) {
                templates = postTemplateRepository.findAllByUserOrderByCreatedAtDesc(user);
            }
        } catch (Exception e) {
            System.err.println("Market sync failed: " + e.getMessage());
        }

        // ✨ 2. Self-Healing: Fix broken templates
        for (PostTemplate template : templates) {
            if (template.getSourceMarketItemId() != null) { // Check if it looks broken (e.g. "Study Record" but no text
                                                            // fields OR old padding)
                boolean isBroken = false;
                if ("공부 기록 템플릿".equals(template.getName())) {
                    if (template.getFloatingTexts() == null || template.getFloatingTexts().isEmpty()) {
                        isBroken = true;
                    } else {
                        // Check for old padding
                        Object padding = template.getStyles().get("padding");
                        if ("3rem".equals(padding)) {
                            isBroken = true; // Trigger update to new padding
                        }

                        // Check for confusing placeholder text or underscores
                        if (template.getFloatingTexts().stream().anyMatch(ft -> "(비워둠)".equals(ft.getText())
                                || (ft.getText() != null && ft.getText().contains("_")))) {
                            isBroken = true;
                        }

                        // Check for missing 'acquired' tag
                        if (template.getTags() == null || !template.getTags().contains("acquired")) {
                            isBroken = true;
                        }
                    }
                }

                if (isBroken) {
                    try {
                        com.project.byeoryback.domain.market.entity.MarketItem marketItem = marketItemRepository
                                .findById(template.getSourceMarketItemId()).orElse(null);
                        if (marketItem != null && marketItem.getContentJson() != null) {
                            PostTemplateDto.CreateRequest freshData = objectMapper
                                    .readValue(marketItem.getContentJson(), PostTemplateDto.CreateRequest.class);

                            template.update(
                                    freshData.getName(),
                                    freshData.getStyles(),
                                    freshData.getStickers(),
                                    freshData.getFloatingTexts(),
                                    freshData.getFloatingImages(),
                                    freshData.getDefaultFontColor(),
                                    freshData.getThumbnailUrl());

                            // Ensure acquired tag is present
                            List<String> tags = template.getTags();
                            if (tags == null)
                                tags = new java.util.ArrayList<>();
                            if (!tags.contains("acquired")) {
                                tags = new java.util.ArrayList<>(tags);
                                tags.add("acquired");
                                template.setTags(tags);
                            }

                            postTemplateRepository.save(template);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }

        List<PostTemplateDto.Response> result = templates.stream()
                .filter(t -> {
                    boolean isHidden = t.getIsHidden() != null && t.getIsHidden();
                    if (Boolean.TRUE.equals(showHidden)) {
                        return isHidden; // Show ONLY hidden items
                    } else {
                        return !isHidden; // Show ONLY active items
                    }
                })
                .map(PostTemplateDto.Response::from)
                .collect(Collectors.toList());

        return result;
    }

    @Transactional
    public PostTemplateDto.Response createTemplate(Long userId, PostTemplateDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PostTemplate template = PostTemplate.builder()
                .user(user)
                .name(request.getName())
                .styles(request.getStyles())
                .stickers(request.getStickers())
                .floatingImages(request.getFloatingImages())
                .defaultFontColor(request.getDefaultFontColor())
                .thumbnailUrl(request.getThumbnailUrl())
                .tags(request.getTags())
                .build();

        return PostTemplateDto.Response.from(postTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long userId, Long templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this template");
        }

        boolean isPurchased = template.getSourceMarketItemId() != null
                || (template.getTags() != null && template.getTags().contains("acquired"));

        if (isPurchased) {
            // ✨ Soft Delete for Purchased Templates

            template.setIsHidden(true);
            postTemplateRepository.save(template);
        } else {
            // Hard Delete for Custom Templates

            postTemplateRepository.delete(template);
        }
    }

    @Transactional
    public void restoreTemplate(Long userId, Long templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to restore this template");
        }

        template.setIsHidden(false);
        postTemplateRepository.save(template);
    }

    public PostTemplateDto.Response getTemplate(Long templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        return PostTemplateDto.Response.from(template);
    }
}
