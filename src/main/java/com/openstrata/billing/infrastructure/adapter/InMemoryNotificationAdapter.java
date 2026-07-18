package com.openstrata.billing.infrastructure.adapter;

import com.openstrata.billing.domain.port.NotificationPort;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** In-memory NotificationPort stand-in for WeCom/DingTalk (records alerts for inspection). */
@Component
public class InMemoryNotificationAdapter implements NotificationPort {

    public static final List<String> SENT = new CopyOnWriteArrayList<>();

    @Override
    public void alert(String title, String message) {
        SENT.add("ALERT " + title + ": " + message);
    }

    @Override
    public void warn(String title, String message) {
        SENT.add("WARN " + title + ": " + message);
    }
}
