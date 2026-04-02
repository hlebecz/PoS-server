package kurs.backend.service;

import kurs.backend.domain.persistence.dao.*;

/**
 * Реестр сервисов — простая замена DI-контейнеру для plain Java окружения.
 *
 * <p>Создаётся один раз при старте сервера и передаётся в обработчики запросов. Все DAO и сервисы —
 * синглтоны внутри реестра.
 *
 * <p>Пример использования:
 *
 * <pre>
 *   ServiceRegistry services = ServiceRegistry.create();
 *   Sale sale = services.getSaleService().processSale(caller, items);
 * </pre>
 */
public class ServiceRegistry {

  private final AuthService authService;
  private final UserService userService;
  private final EmployeeService employeeService;
  private final StockService stockService;
  private final SaleService saleService;
  private final TimesheetService timesheetService;
  private final ReportService reportService;

  private ServiceRegistry(
      AuthService authService,
      UserService userService,
      EmployeeService employeeService,
      StockService stockService,
      SaleService saleService,
      TimesheetService timesheetService,
      ReportService reportService) {
    this.authService = authService;
    this.userService = userService;
    this.employeeService = employeeService;
    this.stockService = stockService;
    this.saleService = saleService;
    this.timesheetService = timesheetService;
    this.reportService = reportService;
  }

  /** Создаёт и связывает все DAO и сервисы. Вызывается один раз при старте приложения. */
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

    StockService stockService = new StockService(stockDao, storeDao);
    UserService userService = new UserService(userDao);

    return new ServiceRegistry(
        new AuthService(userDao, employeeDao, timesheetDao, userService),
        userService,
        new EmployeeService(employeeDao, storeDao),
        stockService,
        new SaleService(saleDao, employeeDao, stockService),
        new TimesheetService(timesheetDao, employeeDao, storeDao),
        new ReportService(storeDao, saleDao, employeeDao, timesheetDao));
  }

  public AuthService getAuthService() {
    return authService;
  }

  public UserService getUserService() {
    return userService;
  }

  public EmployeeService getEmployeeService() {
    return employeeService;
  }

  public StockService getStockService() {
    return stockService;
  }

  public SaleService getSaleService() {
    return saleService;
  }

  public TimesheetService getTimesheetService() {
    return timesheetService;
  }

  public ReportService getReportService() {
    return reportService;
  }
}
