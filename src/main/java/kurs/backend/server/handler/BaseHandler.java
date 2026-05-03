package kurs.backend.server.handler;

import com.google.gson.Gson;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.server.JsonUtil;
import kurs.backend.server.auth.JwtUtil;

/**
 * Базовый обработчик.
 *
 * <p>Содержит общую логику:
 *
 * <ul>
 *   <li>Десериализация payload → DTO через Jackson.
 *   <li>Валидация и парсинг JWT → AuthenticatedUser.
 *   <li>Единообразная обёртка исключений в Response.fail().
 * </ul>
 *
 * <p>Конкретные обработчики наследуют этот класс и реализуют только бизнес-логику в методах вида
 * {@code handleXxx()}.
 */
public abstract class BaseHandler {

  protected static final Gson GSON = JsonUtil.GSON;

  public final Response handleExceptions(Request request, String clientIp) {
    try {
      return handle(request, clientIp);
    } catch (AccessDeniedException e) {
      return Response.fail(request.getRequestId(), e.getMessage(), "ACCESS_DENIED");
    } catch (ServiceException e) {
      return Response.fail(request.getRequestId(), e.getMessage(), e.getErrorCode());
    } catch (IllegalArgumentException e) {
      return Response.fail(request.getRequestId(), e.getMessage(), "INVALID_REQUEST");
    } catch (Exception e) {
      e.printStackTrace(); // увидишь в консоли сервера
      return Response.fail(
          request.getRequestId(),
          e.getClass().getSimpleName() + ": " + e.getMessage(),
          "INTERNAL_ERROR");
      //      return Response.fail(request.getRequestId(), "Внутренняя ошибка сервера",
      // "INTERNAL_ERROR");
    }
  }

  protected abstract Response handle(Request request, String clientIp);

  protected abstract boolean supports(RequestType type);

  protected AuthenticatedUser authenticate(Request request) {
    String token = request.getToken();
    if (token == null || token.isBlank())
      throw new IllegalArgumentException("Токен авторизации отсутствует");
    try {
      return JwtUtil.validateToken(token);
    } catch (Exception e) {
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
