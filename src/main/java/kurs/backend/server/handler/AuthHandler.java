package kurs.backend.server.handler;

import java.util.Set;

import kurs.backend.domain.dto.request.LoginRequest;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.AuthService;

public class AuthHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED = Set.of(RequestType.LOGIN, RequestType.LOGOUT);

  private final AuthService authService;

  public AuthHandler(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    return switch (request.getType()) {
      case LOGIN -> handleLogin(request);
      case LOGOUT -> handleLogout(request);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleLogin(Request request) {
    LoginRequest req = parsePayload(request, LoginRequest.class);
    String token = authService.login(req);
    return Response.ok(request.getRequestId(), "Авторизация успешна", toJson(token));
  }

  private Response handleLogout(Request request) {
    authService.logout(authenticate(request));
    return Response.ok(request.getRequestId(), "Сессия завершена", null);
  }
}
