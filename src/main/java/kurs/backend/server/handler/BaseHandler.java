package kurs.backend.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.server.JsonUtil;
import kurs.backend.server.auth.JwtUtil;

public abstract class BaseHandler {

  private static final Logger log = LogManager.getLogger(BaseHandler.class);
  protected static final Gson GSON = JsonUtil.GSON;

  public final Response handleExceptions(Request request, String clientIp) {
    try {
      return handle(request, clientIp);
    } catch (AccessDeniedException e) {
      log.warn(
          "Access denied for request type: {}, message: {}", request.getType(), e.getMessage());
      return Response.fail(request.getRequestId(), e.getMessage(), "ACCESS_DENIED");
    } catch (ServiceException e) {
      log.warn(
          "Service exception for request type: {}, code: {}, message: {}",
          request.getType(),
          e.getErrorCode(),
          e.getMessage());
      return Response.fail(request.getRequestId(), e.getMessage(), e.getErrorCode());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for type: {}, message: {}", request.getType(), e.getMessage());
      return Response.fail(request.getRequestId(), e.getMessage(), "INVALID_REQUEST");
    } catch (Exception e) {
      log.error(
          "Unexpected error handling request type: {}, requestId: {}",
          request.getType(),
          request.getRequestId(),
          e);
      return Response.fail(
          request.getRequestId(),
          e.getClass().getSimpleName() + ": " + e.getMessage(),
          "INTERNAL_ERROR"
          // request.getRequestId(), "Внутренняя ошибка сервера", "INTERNAL_ERROR"

          );
    }
  }

  protected abstract Response handle(Request request, String clientIp);

  protected abstract boolean supports(RequestType type);

  protected AuthenticatedUser authenticate(Request request) {
    String token = request.getToken();
    if (token == null || token.isBlank()) {
      log.debug("Authentication failed: token is missing for request type: {}", request.getType());
      throw new IllegalArgumentException("Токен авторизации отсутствует");
    }
    try {
      AuthenticatedUser user = JwtUtil.validateToken(token);
      log.debug("User authenticated: userId={}, role={}", user.getUserId(), user.getRole());
      return user;
    } catch (Exception e) {
      log.warn(
          "Authentication failed: invalid or expired token for request type: {}",
          request.getType());
      throw new IllegalArgumentException("Недействительный или просроченный токен");
    }
  }

  protected <T> T parsePayload(Request request, Class<T> clazz) {
    String payload = request.getPayload();
    if (payload == null || payload.isBlank())
      throw new IllegalArgumentException("Тело запроса отсутствует");
    return GSON.fromJson(payload, clazz);
  }

  protected String toJson(Object data) {
    return GSON.toJson(data);
  }
}
