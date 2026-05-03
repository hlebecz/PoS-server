package kurs.backend.server.handler;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;

import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.TimesheetService;

public class TimesheetHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_TIMESHEETS_BY_EMPLOYEE,
          RequestType.GET_TIMESHEETS_BY_EMPLOYEE_PERIOD,
          RequestType.GET_TIMESHEETS_BY_STORE,
          RequestType.GET_ALL_TIMESHEETS,
          RequestType.GET_ALL_TIMESHEETS_BY_PERIOD);

  private final TimesheetService timesheetService;

  public TimesheetHandler(TimesheetService timesheetService) {
    this.timesheetService = timesheetService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_TIMESHEETS_BY_EMPLOYEE -> handleByEmployee(request, caller);
      case GET_TIMESHEETS_BY_EMPLOYEE_PERIOD -> handleByEmployeePeriod(request, caller);
      case GET_TIMESHEETS_BY_STORE -> handleByStore(request, caller);
      case GET_ALL_TIMESHEETS -> handleGetAll(request, caller);
      case GET_ALL_TIMESHEETS_BY_PERIOD -> handleGetAllByPeriod(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleByEmployee(Request request, AuthenticatedUser caller) {
    UUID employeeId = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(), toJson(timesheetService.findByEmployee(caller, employeeId)));
  }

  private Response handleByEmployeePeriod(Request request, AuthenticatedUser caller) {
    JsonObject node = parseNode(request);
    UUID employeeId = UUID.fromString(node.get("id").getAsString());
    LocalDate from = LocalDate.parse(node.get("from").getAsString());
    LocalDate to = LocalDate.parse(node.get("to").getAsString());
    return Response.ok(
        request.getRequestId(),
        toJson(timesheetService.findByEmployeeAndPeriod(caller, employeeId, from, to)));
  }

  private Response handleByStore(Request request, AuthenticatedUser caller) {
    JsonObject node = parseNode(request);
    UUID storeId = UUID.fromString(node.get("id").getAsString());
    LocalDate from = LocalDate.parse(node.get("from").getAsString());
    LocalDate to = LocalDate.parse(node.get("to").getAsString());
    return Response.ok(
        request.getRequestId(), toJson(timesheetService.findByStore(caller, storeId, from, to)));
  }

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(timesheetService.findAll(caller)));
  }

  private Response handleGetAllByPeriod(Request request, AuthenticatedUser caller) {
    JsonObject node = parseNode(request);
    LocalDate from = LocalDate.parse(node.get("from").getAsString());
    LocalDate to = LocalDate.parse(node.get("to").getAsString());
    return Response.ok(
        request.getRequestId(), toJson(timesheetService.findAllByPeriod(caller, from, to)));
  }

  private JsonObject parseNode(Request request) {
    try {
      return GSON.fromJson(request.getPayload(), JsonObject.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Некорректный формат запроса: " + e.getMessage());
    }
  }
}
