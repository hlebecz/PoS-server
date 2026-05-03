package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import kurs.backend.domain.dto.request.CreateUserRequest;
import kurs.backend.domain.dto.request.UpdateUserRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.UserService;

public class UserHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_USERS,
          RequestType.GET_USER,
          RequestType.CREATE_USER,
          RequestType.UPDATE_USER,
          RequestType.DELETE_USER,
          RequestType.DEACTIVATE_USER,
          RequestType.ACTIVATE_USER);

  private final UserService userService;

  public UserHandler(UserService userService) {
    this.userService = userService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_USERS -> handleGetAll(request, caller);
      case GET_USER -> handleGetById(request, caller);
      case CREATE_USER -> handleCreate(request, caller);
      case UPDATE_USER -> handleUpdate(request, caller);
      case DELETE_USER -> handleDelete(request, caller);
      case DEACTIVATE_USER -> handleDeactivate(request, caller);
      case ACTIVATE_USER -> handleActivate(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(userService.findAll(caller)));
  }

  private Response handleGetById(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(userService.findById(caller, id)));
  }

  private Response handleCreate(Request request, AuthenticatedUser caller) {
    CreateUserRequest req = parsePayload(request, CreateUserRequest.class);
    return Response.ok(
        request.getRequestId(), "Пользователь создан", toJson(userService.create(caller, req)));
  }

  private Response handleUpdate(Request request, AuthenticatedUser caller) {
    UpdateUserRequest req = parsePayload(request, UpdateUserRequest.class);
    return Response.ok(
        request.getRequestId(), "Пользователь обновлён", toJson(userService.update(caller, req)));
  }

  private Response handleDelete(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    userService.delete(caller, id);
    return Response.ok(request.getRequestId(), "Пользователь удалён", null);
  }

  private Response handleDeactivate(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(),
        "Пользователь деактивирован",
        toJson(userService.deactivate(caller, id)));
  }

  private Response handleActivate(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(),
        "Пользователь активирован",
        toJson(userService.activate(caller, id)));
  }
}
