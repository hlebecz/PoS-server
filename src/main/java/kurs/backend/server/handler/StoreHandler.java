package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import kurs.backend.domain.dto.request.CreateStoreRequest;
import kurs.backend.domain.dto.request.UpdateStoreRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.StoreService;

public class StoreHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_STORES,
          RequestType.GET_STORES_ACTIVE,
          RequestType.GET_STORES_ACTIVE_BASIC,
          RequestType.GET_STORE,
          RequestType.GET_STORES_BY_WAREHOUSE,
          RequestType.CREATE_STORE,
          RequestType.UPDATE_STORE,
          RequestType.DELETE_STORE,
          RequestType.DEACTIVATE_STORE,
          RequestType.ACTIVATE_STORE);

  private final StoreService storeService;

  public StoreHandler(StoreService storeService) {
    this.storeService = storeService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_STORES -> handleGetAll(request, caller);
      case GET_STORES_ACTIVE -> handleGetAllActive(request, caller);
      case GET_STORES_ACTIVE_BASIC -> handleGetAllActiveBasic(request, caller);
      case GET_STORE -> handleGetById(request, caller);
      case GET_STORES_BY_WAREHOUSE -> handleGetByWarehouse(request, caller);
      case CREATE_STORE -> handleCreate(request, caller);
      case UPDATE_STORE -> handleUpdate(request, caller);
      case DELETE_STORE -> handleDelete(request, caller);
      case DEACTIVATE_STORE -> handleDeactivate(request, caller);
      case ACTIVATE_STORE -> handleActivate(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(storeService.findAll(caller)));
  }

  private Response handleGetAllActive(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(storeService.findAllActive(caller)));
  }

  private Response handleGetAllActiveBasic(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(storeService.findAllActiveBasic(caller)));
  }

  private Response handleGetById(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(storeService.findById(caller, id)));
  }

  private Response handleGetByWarehouse(Request request, AuthenticatedUser caller) {
    UUID warehouseId = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(), toJson(storeService.findByWarehouse(caller, warehouseId)));
  }

  private Response handleCreate(Request request, AuthenticatedUser caller) {
    CreateStoreRequest req = parsePayload(request, CreateStoreRequest.class);
    return Response.ok(
        request.getRequestId(), "Магазин создан", toJson(storeService.create(caller, req)));
  }

  private Response handleUpdate(Request request, AuthenticatedUser caller) {
    UpdateStoreRequest req = parsePayload(request, UpdateStoreRequest.class);
    return Response.ok(
        request.getRequestId(), "Магазин обновлён", toJson(storeService.update(caller, req)));
  }

  private Response handleDelete(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    storeService.delete(caller, id);
    return Response.ok(request.getRequestId(), "Магазин удалён", null);
  }

  private Response handleDeactivate(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(),
        "Магазин деактивирован",
        toJson(storeService.deactivate(caller, id)));
  }

  private Response handleActivate(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(), "Магазин активирован", toJson(storeService.activate(caller, id)));
  }
}
