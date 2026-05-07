package kurs.backend.server.handler;

import java.util.Set;
import java.util.UUID;

import kurs.backend.domain.dto.request.CreateProductRequest;
import kurs.backend.domain.dto.request.UpdateProductRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.ProductService;

public class ProductHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.GET_PRODUCTS,
          RequestType.GET_PRODUCT,
          RequestType.GET_PRODUCT_BY_ARTICLE,
          RequestType.CREATE_PRODUCT,
          RequestType.UPDATE_PRODUCT,
          RequestType.DELETE_PRODUCT);

  private final ProductService productService;

  public ProductHandler(ProductService productService) {
    this.productService = productService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case GET_PRODUCTS -> handleGetAll(request, caller);
      case GET_PRODUCT -> handleGetById(request, caller);
      case GET_PRODUCT_BY_ARTICLE -> handleGetByArticle(request, caller);
      case CREATE_PRODUCT -> handleCreate(request, caller);
      case UPDATE_PRODUCT -> handleUpdate(request, caller);
      case DELETE_PRODUCT -> handleDelete(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  private Response handleGetAll(Request request, AuthenticatedUser caller) {
    return Response.ok(request.getRequestId(), toJson(productService.findAll(caller)));
  }

  private Response handleGetById(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    return Response.ok(request.getRequestId(), toJson(productService.findById(caller, id)));
  }

  private Response handleGetByArticle(Request request, AuthenticatedUser caller) {
    String article = parsePayload(request, String.class);
    return Response.ok(
        request.getRequestId(), toJson(productService.findByArticle(caller, article)));
  }

  private Response handleCreate(Request request, AuthenticatedUser caller) {
    CreateProductRequest req = parsePayload(request, CreateProductRequest.class);
    return Response.ok(request.getRequestId(), toJson(productService.create(caller, req)));
  }

  private Response handleUpdate(Request request, AuthenticatedUser caller) {
    UpdateProductRequest req = parsePayload(request, UpdateProductRequest.class);
    return Response.ok(request.getRequestId(), toJson(productService.update(caller, req)));
  }

  private Response handleDelete(Request request, AuthenticatedUser caller) {
    UUID id = parsePayload(request, UUID.class);
    productService.delete(caller, id);
    return Response.ok(request.getRequestId(), "Товар удален");
  }
}
