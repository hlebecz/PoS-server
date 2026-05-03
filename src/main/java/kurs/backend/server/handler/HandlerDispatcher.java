package kurs.backend.server.handler;

import java.util.List;

import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.Response;

public class HandlerDispatcher {

  private final List<BaseHandler> handlers;

  public HandlerDispatcher(List<BaseHandler> handlers) {
    this.handlers = handlers;
  }

  public Response dispatch(Request request, String clientIp) {
    if (request == null || request.getType() == null)
      return Response.fail(null, "Тип запроса не указан", "INVALID_REQUEST");

    return handlers.stream()
        .filter(h -> h.supports(request.getType()))
        .findFirst()
        .map(h -> h.handleExceptions(request, clientIp))
        .orElse(
            Response.fail(
                request.getRequestId(),
                "Неизвестный тип запроса: " + request.getType(),
                "UNKNOWN_REQUEST_TYPE"));
  }
}
