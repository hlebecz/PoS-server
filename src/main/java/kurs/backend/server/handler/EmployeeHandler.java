package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import kurs.backend.domain.dto.request.CreateEmployeeRequest;
import kurs.backend.domain.dto.request.FireEmployeeRequest;
import kurs.backend.domain.dto.request.UpdateEmployeeRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.EmployeeService;

public class EmployeeHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_EMPLOYEES,
          RequestType.GET_EMPLOYEES_BY_STORE,
          RequestType.GET_EMPLOYEE,
          RequestType.CREATE_EMPLOYEE,
          RequestType.UPDATE_EMPLOYEE,
          RequestType.DELETE_EMPLOYEE,
          RequestType.FIRE_EMPLOYEE);

  private final EmployeeService employeeService;

  public EmployeeHandler(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_EMPLOYEES -> handleGetAll(request, caller);
      case GET_EMPLOYEES_BY_STORE -> handleGetByStore(request, caller);
      case GET_EMPLOYEE -> handleGetById(request, caller);
      case CREATE_EMPLOYEE -> handleCreate(request, caller);
      case UPDATE_EMPLOYEE -> handleUpdate(request, caller);
      case DELETE_EMPLOYEE -> handleDelete(request, caller);
      case FIRE_EMPLOYEE -> handleFire(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  // -----------------------------------------------------------------------

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(employeeService.findAll(caller)));
  }

  private Response handleGetByStore(Request request, AuthenticatedUser caller) {
    UUID storeId = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(), toJson(employeeService.findByStore(caller, storeId)));
  }

  private Response handleGetById(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(employeeService.findById(caller, id)));
  }

  private Response handleCreate(Request request, AuthenticatedUser caller) {
    CreateEmployeeRequest req = parsePayload(request, CreateEmployeeRequest.class);
    return Response.ok(
        request.getRequestId(), "Сотрудник создан", toJson(employeeService.create(caller, req)));
  }

  private Response handleUpdate(Request request, AuthenticatedUser caller) {
    UpdateEmployeeRequest req = parsePayload(request, UpdateEmployeeRequest.class);
    return Response.ok(
        request.getRequestId(), "Сотрудник обновлён", toJson(employeeService.update(caller, req)));
  }

  private Response handleDelete(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    employeeService.delete(caller, id);
    return Response.ok(request.getRequestId(), "Сотрудник удалён", null);
  }

  /**
   * Payload: { "employeeId": "...", "firedAt": "2025-06-01" } firedAt — опционально, если не
   * передан — используется today.
   */
  private Response handleFire(Request request, AuthenticatedUser caller) {
    FireEmployeeRequest req = parsePayload(request, FireEmployeeRequest.class);
    return Response.ok(
        request.getRequestId(), "Сотрудник уволен", toJson(employeeService.fire(caller, req)));
  }
}
