package cc.openstrata.billing.domain.port;

/** Alerting SPI (§4.8). WeCom/DingTalk adapter (optional) for budget alerts. */
public interface NotificationPort {
    void alert(String title, String message);
    void warn(String title, String message);
}
