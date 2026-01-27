package com.project.byeoryback.domain.setting.widget.repository;

import com.project.byeoryback.domain.setting.widget.entity.WidgetPreset;
import com.project.byeoryback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WidgetPresetRepository extends JpaRepository<WidgetPreset, Long> {
    List<WidgetPreset> findAllByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
