package kurs.backend.server;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ServerInfo {

  private static final ServerInfo INSTANCE = new ServerInfo();

  private final Instant startedAt = Instant.now();
  private final AtomicInteger activeConnections = new AtomicInteger(0);
  private final AtomicLong totalRequestsHandled = new AtomicLong(0);
  private final AtomicLong totalErrors = new AtomicLong(0);

  private ServerInfo() {}

  public static ServerInfo getInstance() {
    return INSTANCE;
  }

  public void onConnect() {
    activeConnections.incrementAndGet();
  }

  public void onDisconnect() {
    activeConnections.decrementAndGet();
  }

  public void onRequest() {
    totalRequestsHandled.incrementAndGet();
  }

  public void onError() {
    totalErrors.incrementAndGet();
  }

  public int getActiveConnections() {
    return activeConnections.get();
  }

  public long getTotalRequests() {
    return totalRequestsHandled.get();
  }

  public long getTotalErrors() {
    return totalErrors.get();
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public long uptimeSeconds() {
    return Instant.now().getEpochSecond() - startedAt.getEpochSecond();
  }

  public LocalDateTime getStartedAtLocal() {
    return LocalDateTime.ofInstant(startedAt, ZoneId.systemDefault());
  }

  public long usedMemoryMb() {
    Runtime rt = Runtime.getRuntime();
    return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
  }

  public long maxMemoryMb() {
    return Runtime.getRuntime().maxMemory() / (1024 * 1024);
  }

  public int availableProcessors() {
    return Runtime.getRuntime().availableProcessors();
  }

  public String getJvmUptime() {
    long seconds = uptimeSeconds();
    long h = seconds / 3600;
    long m = (seconds % 3600) / 60;
    long s = seconds % 60;
    return String.format("%02d:%02d:%02d", h, m, s);
  }
}
