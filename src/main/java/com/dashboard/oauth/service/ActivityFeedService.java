package com.dashboard.oauth.service;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityFeedService implements IActivityFeedService {

    private static final int MAX_EVENTS = 100; // Keep last 100 events in memory
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentLinkedDeque<ActivityEvent> recentEvents = new ConcurrentLinkedDeque<>();

    @Override
    public void publishEvent(ActivityEvent event) {
        // Add to in-memory store
        recentEvents.addFirst(event);

        // Trim to max size
        while (recentEvents.size() > MAX_EVENTS) {
            recentEvents.removeLast();
        }

        // Broadcast to all connected WebSocket clients
        messagingTemplate.convertAndSend("/topic/activity", event);
    }

    @Override
    public List<ActivityEvent> getRecentEvents(int limit) {
        return recentEvents.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}