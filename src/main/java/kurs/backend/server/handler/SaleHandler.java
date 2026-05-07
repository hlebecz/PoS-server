package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import kurs.backend.domain.dto.request.CreateSaleRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.SaleService;

public class SaleHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_SALES_BY_STORE,
          RequestType.GET_SALE,
          RequestType.PROCESS_SALE,
          RequestType.PROCESS_RETURN);

  private final SaleService saleService;

  public SaleHandler(SaleService saleService) {
    this.saleService = saleService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_SALES_BY_STORE -> handleGetByStore(request, caller);
      case GET_SALE -> handleGetById(request, caller);
      case PROCESS_SALE -> handleProcessSale(request, caller);
      case PROCESS_RETURN -> handleProcessReturn(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleGetByStore(Request request, AuthenticatedUser caller) {
    UUID storeId = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(saleService.findByStore(caller, storeId)));
  }

  private Response handleGetById(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(saleService.findById(caller, id)));
  }

  private Response handleProcessSale(Request request, AuthenticatedUser caller) {
    CreateSaleRequest req = parsePayload(request, CreateSaleRequest.class);
    return Response.ok(
        request.getRequestId(), "Продажа проведена", toJson(saleService.processSale(caller, req)));
  }

  private Response handleProcessReturn(Request request, AuthenticatedUser caller) {
    UUID originalSaleId = parsePayload(request, UUID.class);
    return Response.ok(
        request.getRequestId(),
        "Возврат оформлен",
        toJson(saleService.processReturn(caller, originalSaleId)));
  }
}
