package kurs.backend.server.handler;

import java.util.List;

import kurs.backend.domain.service.ServiceRegistry;
import kurs.backend.server.ServerInfo;

public class HandlerRegistry {

  private HandlerRegistry() {}

  public static HandlerDispatcher create(
      ServiceRegistry services, ServerInfo serverInfo, Runnable shutdownCallback) {

    List<BaseHandler> handlers =
        List.of(
            new SystemHandler(serverInfo, shutdownCallback),
            new AuthHandler(services.getAuthService()),
            new UserHandler(services.getUserService()),
            new EmployeeHandler(services.getEmployeeService()),
            new StoreHandler(services.getStoreService()),
            new WarehouseHandler(services.getWarehouseService()),
            new StockHandler(services.getStockService()),
            new SaleHandler(services.getSaleService()),
            new TimesheetHandler(services.getTimesheetService()),
            new ReportHandler(services.getReportService()));

    return new HandlerDispatcher(handlers);
  }
}
