package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import kurs.backend.domain.dto.request.CreateWarehouseRequest;
import kurs.backend.domain.dto.request.UpdateWarehouseRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.WarehouseService;

public class WarehouseHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_WAREHOUSES,
          RequestType.GET_WAREHOUSES_ACTIVE,
          RequestType.GET_WAREHOUSES_ACTIVE_BASIC,
          RequestType.GET_WAREHOUSE,
          RequestType.CREATE_WAREHOUSE,
          RequestType.UPDATE_WAREHOUSE,
          RequestType.DELETE_WAREHOUSE,
          RequestType.DEACTIVATE_WAREHOUSE,
          RequestType.ACTIVATE_WAREHOUSE);

  private final WarehouseService warehouseService;

  public WarehouseHandler(WarehouseService warehouseService) {
    this.warehouseService = warehouseService;
  }

  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_WAREHOUSES -> handleGetAll(request, caller);
      case GET_WAREHOUSES_ACTIVE -> handleGetAllActive(request, caller);
      case GET_WAREHOUSES_ACTIVE_BASIC -> handleGetAllActiveBasic(request, caller);
      case GET_WAREHOUSE -> handleGetById(request, caller);
      case CREATE_WAREHOUSE -> handleCreate(request, caller);
      case UPDATE_WAREHOUSE -> handleUpdate(request, caller);
      case DELETE_WAREHOUSE -> handleDelete(request, caller);
      case DEACTIVATE_WAREHOUSE -> handleDeactivate(request, caller);
      case ACTIVATE_WAREHOUSE -> handleActivate(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(warehouseService.findAll(caller)));
  }

  private Response handleGetAllActive(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(warehouseService.findAllActive(caller)));
  }

  private Response handleGetAllActiveBasic(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(warehouseService.findAllActiveBasic(caller)));
  }

  private Response handleGetById(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(warehouseService.findById(caller, id)));
  }

  private Response handleCreate(Request request, AuthenticatedUser caller) {
    CreateWarehouseRequest req = parsePayload(request, CreateWarehouseRequest.class);
    return Response.ok(
        request.getRequestId(), "Склад создан", toJson(warehouseService.create(caller, req)));
  }

  private Response handleUpdate(Request request, AuthenticatedUser caller) {
    UpdateWarehouseRequest req = parsePayload(request, UpdateWarehouseRequest.class);
    return Response.ok(
        request.getRequestId(), "Склад обновлён", toJson(warehouseService.update(caller, req)));
  }

  private Response handleDelete(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    warehouseService.delete(caller, id);
    return Response.ok(request.getRequestId(), "Склад удалён", null);
  }

  private Response handleDeactivate(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(),
        "Склад деактивирован",
        toJson(warehouseService.deactivate(caller, id)));
  }

  private Response handleActivate(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(),
        "Склад деактивирован",
        toJson(warehouseService.activate(caller, id)));
  }
}
