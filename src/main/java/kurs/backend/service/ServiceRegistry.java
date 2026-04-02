package kurs.backend.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import kurs.backend.domain.persistence.dao.*;

/**
 * Реестр сервисов — единая точка сборки для plain Java окружения (без DI-контейнера). Создаётся
 * один раз при старте сервера.
 *
 * <pre>
 *   ServiceRegistry services = ServiceRegistry.create();
 *   // в обработчике запроса:
 *   SaleResponse sale = services.getSaleService().processSale(caller, req);
 * </pre>
 */
@Getter
@AllArgsConstructor
public class ServiceRegistry {

  private final AuthService authService;
  private final UserService userService;
  private final EmployeeService employeeService;
  private final StoreService storeService;
  private final WarehouseService warehouseService;
  private final StockService stockService;
  private final SaleService saleService;
  private final TimesheetService timesheetService;
  private final ReportService reportService;

  /** Инициализирует все DAO и сервисы, связывая зависимости вручную. */
  public static ServiceRegistry create() {

    UserDao userDao = new UserDao();
    EmployeeDao employeeDao = new EmployeeDao();
    StoreDao storeDao = new StoreDao();
    WarehouseDao warehouseDao = new WarehouseDao();
    ProductDao productDao = new ProductDao();
    StockDao stockDao = new StockDao();
    SaleDao saleDao = new SaleDao();
    TimesheetDao timesheetDao = new TimesheetDao();
    LocationDao locationDao = new LocationDao();

    StockService stockService = new StockService(stockDao, storeDao, warehouseDao, productDao);
    UserService userService = new UserService(userDao);

    return new ServiceRegistry(
        new AuthService(userDao, employeeDao, timesheetDao, userService),
        userService,
        new EmployeeService(employeeDao, storeDao, userDao, locationDao),
        new StoreService(storeDao, userDao, warehouseDao, locationDao),
        new WarehouseService(warehouseDao, locationDao),
        stockService,
        new SaleService(saleDao, employeeDao, productDao, stockService),
        new TimesheetService(timesheetDao, employeeDao, storeDao),
        new ReportService(storeDao, saleDao, employeeDao, timesheetDao));
  }
}
