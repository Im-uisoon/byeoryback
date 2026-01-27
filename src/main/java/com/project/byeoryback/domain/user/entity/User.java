package com.project.byeoryback.domain.user.entity;

import com.project.byeoryback.domain.customtwidget.entity.CustomWidget;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.project.byeoryback.domain.user.enums.AuthProvider;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile userProfile;

    @OneToOne(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private com.project.byeoryback.domain.persona.entity.Persona persona;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.post.entity.PostLike> likes = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.market.entity.Review> reviews = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.market.entity.MarketTransaction> purchases = new java.util.ArrayList<>();

    // Seller transactions might be tricky if we want to keep history, but for
    // complete deletion:
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.market.entity.MarketTransaction> sales = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.message.entity.Message> messages = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.todo.entity.Todo> todos = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.market.entity.Wishlist> wishlists = new java.util.ArrayList<>();

    // 🌟 1. Market Listings (Items sold by user)
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.market.entity.MarketItem> marketListings = new java.util.ArrayList<>();

    // 🌟 2. Widgets created by user
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<CustomWidget> customWidgets = new java.util.ArrayList<>();

    // 🌟 3. Room Cycle Memberships (User's participation in cycles)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.project.byeoryback.domain.room.entity.RoomCycleMember> cycleMemberships = new java.util.ArrayList<>();

    // 🌟 4. Pin (Security/Lock)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private com.project.byeoryback.domain.pin.entity.Pin pin;

    @Column(nullable = false, unique = true)
    private String email;

    @Column // password can be null for OAuth2 users
    private String password;

    @Column(nullable = false)
    private String role; // e.g., "ROLE_USER"

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean fullProfile = false;

    @Column(nullable = false)
    @Builder.Default
    private Long todayPlayTime = 0L; // Seconds

    @Column(nullable = false)
    @Builder.Default
    private Long credits = 0L;

    private java.time.LocalDate lastPlayDate;

    public void completeProfile() {
        this.fullProfile = true;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void recordHeartbeat(java.time.LocalDate today, Long incrementSeconds) {
        if (this.lastPlayDate == null || !this.lastPlayDate.equals(today)) {
            this.todayPlayTime = 0L;
            this.lastPlayDate = today;
        }
        this.todayPlayTime += incrementSeconds;
    }

    public void addCredits(Long amount) {
        this.credits += amount;
    }

    public boolean spendCredits(Long amount) {
        if (this.credits < amount) {
            return false; // Not enough credits
        }
        this.credits -= amount;
        return true;
    }

    public void removePin() {
        this.pin = null;
    }

}
