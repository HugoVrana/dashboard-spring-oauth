package com.dashboard.oauth.controller.v2;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Activity", description = "Activity feed operations")
@RequestMapping(value = "/api/v2/activity", produces = "application/json")
public class V2ActivityController {

    private final IActivityFeedService activityFeedService;

    @Operation(summary = "Get recent activity events",
            description = "Retrieves the most recent activity events from the feed")
    @GetMapping("/recent")
    public List<ActivityEvent> getRecentActivity(
            @RequestParam(defaultValue = "50") int limit) {
        return activityFeedService.getRecentEvents(limit);
    }
}
