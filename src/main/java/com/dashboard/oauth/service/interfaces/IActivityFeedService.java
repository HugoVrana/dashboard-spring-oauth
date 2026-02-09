package com.dashboard.oauth.service.interfaces;

import com.dashboard.common.model.ActivityEvent;

import java.util.List;

public interface IActivityFeedService {
    void publishEvent(ActivityEvent event);

    List<ActivityEvent> getRecentEvents(int limit);
}
