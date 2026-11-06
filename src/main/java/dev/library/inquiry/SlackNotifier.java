package dev.library.inquiry;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
class SlackNotifier {
  private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);
  private final URI webhook;
  private final RestClient client;

  SlackNotifier(@Value("${library-dashboard.slack.webhook-url:}") String url) {
    this.webhook = parse(url);
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(3));
    this.client = RestClient.builder().requestFactory(factory).build();
  }

  private static URI parse(String url) {
    if (url == null || url.isBlank()) return null;
    try {
      URI uri = new URI(url.strip());
      if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null)
        throw new IllegalArgumentException();
      return uri;
    } catch (Exception e) {
      // Never log the URL or the parser message: the URL is the secret.
      log.warn("Slack webhook URL is invalid; inquiry notifications are disabled");
      return null;
    }
  }

  void sendAfterCommit(Map<String, Object> payload) {
    if (webhook == null) return;
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        // ponytail: 가상 스레드 즉시 전송, Pod 종료 시 유실 가능(DB엔 남음). 필요하면 큐/재시도
        Thread.ofVirtual().name("slack-notify").start(() -> send(payload));
      }
    });
  }

  private void send(Map<String, Object> payload) {
    try {
      client.post().uri(webhook).contentType(MediaType.APPLICATION_JSON).body(payload)
          .retrieve().toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.warn("Slack notification failed: HTTP {}", e.getStatusCode().value());
    } catch (Throwable e) {
      log.warn("Slack notification failed: {}", e.getClass().getSimpleName());
    }
  }

  static String escape(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
