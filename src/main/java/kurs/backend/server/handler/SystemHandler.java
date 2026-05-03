package kurs.backend.server.handler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.server.ServerInfo;

public class SystemHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(RequestType.PING, RequestType.HEALTH, RequestType.SERVER_STATUS, RequestType.SHUTDOWN);

  private final ServerInfo serverInfo;

  /** Callback на shutdown — передаётся из Server, чтобы не хранить ссылку на сервер. */
  private final Runnable shutdownCallback;

  public SystemHandler(ServerInfo serverInfo, Runnable shutdownCallback) {
    this.serverInfo = serverInfo;
    this.shutdownCallback = shutdownCallback;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    return switch (request.getType()) {
      case PING -> handlePing(request);
      case HEALTH -> handleHealth(request);
      case SERVER_STATUS -> handleServerStatus(request);
      case SHUTDOWN -> handleShutdown(request);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handlePing(Request request) {
    return Response.ok(request.getRequestId(), "PONG", null);
  }

  private Response handleHealth(Request request) {
    authenticate(request); // токен нужен, роль — нет

    Map<String, Object> health = new LinkedHashMap<>();
    health.put("status", "UP");
    health.put("startedAt", serverInfo.getStartedAtLocal().toString());
    health.put("uptime", serverInfo.getJvmUptime());
    health.put("activeConnections", serverInfo.getActiveConnections());

    return Response.ok(request.getRequestId(), toJson(health));
  }

  private Response handleServerStatus(Request request) {
    AuthenticatedUser caller = authenticate(request);
    requireAdmin(caller, request);

    Map<String, Object> status = new LinkedHashMap<>();
    status.put("status", "UP");
    status.put("startedAt", serverInfo.getStartedAtLocal().toString());
    status.put("uptime", serverInfo.getJvmUptime());
    status.put("activeConnections", serverInfo.getActiveConnections());
    status.put("totalRequestsHandled", serverInfo.getTotalRequests());
    status.put("totalErrors", serverInfo.getTotalErrors());
    status.put("usedMemoryMb", serverInfo.usedMemoryMb());
    status.put("maxMemoryMb", serverInfo.maxMemoryMb());
    status.put("availableProcessors", serverInfo.availableProcessors());
    status.put("reportedAt", LocalDateTime.now().toString());

    return Response.ok(request.getRequestId(), toJson(status));
  }

  private Response handleShutdown(Request request) {
    AuthenticatedUser caller = authenticate(request);
    requireAdmin(caller, request);

    Thread.ofVirtual()
        .start(
            () -> {
              try {
                Thread.sleep(300);
              } catch (InterruptedException ignored) {
              }
              shutdownCallback.run();
            });

    return Response.ok(request.getRequestId(), "Сервер завершает работу", null);
  }

  private void requireAdmin(AuthenticatedUser caller, Request request) {
    if (!caller.isAdmin()) throw new AccessDeniedException("Требуется роль ADMIN");
  }
}
