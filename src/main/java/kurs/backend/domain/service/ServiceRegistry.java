package kurs.backend.domain.service;

import org.hibernate.SessionFactory;

import lombok.AllArgsConstructor;
import lombok.Getter;

import kurs.backend.domain.persistence.HibernateUtil;
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
  private final ProductService productService;
  private final StockService stockService;
  private final SaleService saleService;
  private final TimesheetService timesheetService;
  private final ReportService reportService;

  /** Инициализирует все DAO и сервисы, связывая зависимости вручную. */
  public static ServiceRegistry create() {

    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    UserDao userDao = new UserDao(sessionFactory);
    EmployeeDao employeeDao = new EmployeeDao(sessionFactory);
    StoreDao storeDao = new StoreDao(sessionFactory);
    WarehouseDao warehouseDao = new WarehouseDao(sessionFactory);
    ProductDao productDao = new ProductDao(sessionFactory);
    StockDao stockDao = new StockDao(sessionFactory);
    SaleDao saleDao = new SaleDao(sessionFactory);
    TimesheetDao timesheetDao = new TimesheetDao(sessionFactory);
    LocationDao locationDao = new LocationDao(sessionFactory);

    StockService stockService = new StockService(stockDao, storeDao, warehouseDao, productDao);
    UserService userService = new UserService(userDao, employeeDao);
    ProductService productService = new ProductService(productDao);

    return new ServiceRegistry(
        new AuthService(userDao, employeeDao, timesheetDao, userService),
        userService,
        new EmployeeService(employeeDao, storeDao, userDao, locationDao),
        new StoreService(storeDao, userDao, warehouseDao, locationDao),
        new WarehouseService(warehouseDao, locationDao),
        productService,
        stockService,
        new SaleService(saleDao, employeeDao, productDao, stockService),
        new TimesheetService(timesheetDao, employeeDao, storeDao),
        new ReportService(storeDao, saleDao, employeeDao, timesheetDao));
  }
}
