package kurs.backend.server.handler;

import java.util.Set;

import kurs.backend.domain.dto.request.ReportRequest;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.model.Request;
import kurs.backend.domain.model.RequestType;
import kurs.backend.domain.model.Response;
import kurs.backend.domain.service.ReportService;

public class ReportHandler extends BaseHandler {

  private static final Set<RequestType> SUPPORTED =
      Set.of(
          RequestType.REPORT_SALES,
          RequestType.REPORT_EMPLOYEE_EFFICIENCY,
          RequestType.REPORT_STORE_EFFICIENCY);

  private final ReportService reportService;

  public ReportHandler(ReportService reportService) {
    this.reportService = reportService;
  }

  @Override
  public boolean supports(RequestType type) {
    return SUPPORTED.contains(type);
  }

  @Override
  protected Response handle(Request request, String clientIp) {
    AuthenticatedUser caller = authenticate(request);
    return switch (request.getType()) {
      case REPORT_SALES -> handleSalesReport(request, caller);
      case REPORT_EMPLOYEE_EFFICIENCY -> handleEmployeeEfficiency(request, caller);
      case REPORT_STORE_EFFICIENCY -> handleStoreEfficiency(request, caller);
      default -> Response.fail(request.getRequestId(), "Неподдерживаемый тип", "UNSUPPORTED");
    };
  }

  // -----------------------------------------------------------------------

  private Response handleSalesReport(Request request, AuthenticatedUser caller) {
    ReportRequest req = parsePayload(request, ReportRequest.class);
    return Response.ok(request.getRequestId(), toJson(reportService.salesReport(caller, req)));
  }

  private Response handleEmployeeEfficiency(Request request, AuthenticatedUser caller) {
    ReportRequest req = parsePayload(request, ReportRequest.class);
    if (req.getEmployeeId() == null) {
      return Response.ok(
          request.getRequestId(), toJson(reportService.employeeEfficiency(caller, req)));
    }
    return Response.ok(
        request.getRequestId(), toJson(reportService.employeeEfficiencyById(caller, req)));
  }

  private Response handleStoreEfficiency(Request request, AuthenticatedUser caller) {
    ReportRequest req = parsePayload(request, ReportRequest.class);
    return Response.ok(request.getRequestId(), toJson(reportService.storeEfficiency(caller, req)));
  }
}
