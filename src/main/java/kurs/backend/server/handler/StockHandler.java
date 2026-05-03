package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;

import kurs.backend.domain.dto.request.SetStockRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.StockService;

public class StockHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_STOCK,
          RequestType.GET_STOCK_BY_LOCATION,
          RequestType.GET_STOCK_BY_PRODUCT,
          RequestType.SET_STOCK,
          RequestType.DELETE_STOCK);

  private final StockService stockService;

  public StockHandler(StockService stockService) {
    this.stockService = stockService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_STOCK -> handleGetAll(request, caller);
      case GET_STOCK_BY_LOCATION -> handleGetByLocation(request, caller);
      case GET_STOCK_BY_PRODUCT -> handleGetByProduct(request, caller);
      case SET_STOCK -> handleSet(request, caller);
      case DELETE_STOCK -> handleDelete(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(stockService.findAll(caller)));
  }

  private Response handleGetByLocation(Request request, AuthenticatedUser caller) {
    UUID locationId = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(), toJson(stockService.findByStorageLocation(caller, locationId)));
  }

  private Response handleGetByProduct(Request request, AuthenticatedUser caller) {
    UUID productId = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(), toJson(stockService.findByProduct(caller, productId)));
  }

  private Response handleSet(Request request, AuthenticatedUser caller) {
    SetStockRequest req = parsePayload(request, SetStockRequest.class);
    return Response.ok(
        request.getRequestId(), "Остаток обновлён", toJson(stockService.set(caller, req)));
  }

  /** Payload: { "storageLocationId": "...", "productId": "..." } */
  private Response handleDelete(Request request, AuthenticatedUser caller) {
    try {
      JsonObject node = GSON.fromJson(request.getPayload(), JsonObject.class);
      UUID storageLocationId = UUID.fromString(node.get("storageLocationId").getAsString());
      UUID productId = UUID.fromString(node.get("productId").getAsString());
      stockService.delete(caller, storageLocationId, productId);
      return Response.ok(request.getRequestId(), "Запись остатка удалена", null);
    } catch (Exception e) {
      throw new IllegalArgumentException("Некорректный формат запроса: " + e.getMessage());
    }
  }
}
