package kurs.backend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import kurs.backend.domain.persistence.entity.*;

/** Test data builder utility for creating test entities with sensible defaults. */
public class TestDataBuilder {

  // User builders
  public static User.UserBuilder user() {
    return User.builder()
        .login("testuser")
        .passwordHash("$2a$12$hashedpassword")
        .role(UserRole.GUEST)
        .isActive(true);
  }

  public static User.UserBuilder adminUser() {
    return user().login("admin").role(UserRole.ADMIN);
  }

  public static User.UserBuilder cashierUser() {
    return user().login("cashier").role(UserRole.CASHIER);
  }

  public static User.UserBuilder managerUser() {
    return user().login("manager").role(UserRole.MANAGER);
  }

  public static User.UserBuilder accountantUser() {
    return user().login("accountant").role(UserRole.ACCOUNTANT);
  }

  // Location builder
  public static Location.LocationBuilder location() {
    return Location.builder()
        .x(BigDecimal.valueOf(50.4501))
        .y(BigDecimal.valueOf(30.5234))
        .address("789 Location Blvd")
        .city("Test City");
  }

  // Warehouse builder
  public static Warehouse warehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.setName("Test Warehouse");
    warehouse.setPhone("555-0002");
    warehouse.setIsActive(true);
    return warehouse;
  }

  public static Warehouse warehouseWithLocation(Location location) {
    Warehouse warehouse = warehouse();
    warehouse.setLocation(location);
    return warehouse;
  }

  // Store builders
  public static Store store() {
    Store store = new Store();
    store.setName("Test Store");
    store.setPhone("555-0001");
    store.setIsActive(true);
    return store;
  }

  public static Store storeWithManager(User manager) {
    Store store = store();
    store.setManager(manager);
    return store;
  }

  public static Store storeWithWarehouse(Warehouse warehouse) {
    Store store = store();
    store.setWarehouse(warehouse);
    return store;
  }

  public static Store storeWithLocation(Location location) {
    Store store = store();
    store.setLocation(location);
    return store;
  }

  public static Store storeComplete(User manager, Warehouse warehouse, Location location) {
    Store store = store();
    store.setManager(manager);
    store.setWarehouse(warehouse);
    store.setLocation(location);
    return store;
  }

  // Employee builders
  public static Employee.EmployeeBuilder employee() {
    return Employee.builder()
        .fullName("John Doe")
        .position("Cashier")
        .hourlyRate(BigDecimal.valueOf(25.00))
        .hiredAt(LocalDate.now());
  }

  public static Employee.EmployeeBuilder employeeWithStore(Store store) {
    return employee().store(store);
  }

  public static Employee.EmployeeBuilder employeeWithUser(User user) {
    return employee().user(user);
  }

  public static Employee.EmployeeBuilder employeeComplete(
      Store store, User user, Location location) {
    return employee().store(store).user(user).location(location);
  }

  // Product builder
  public static Product.ProductBuilder product() {
    return Product.builder()
        .name("Test Product")
        .article("TEST-SKU-001")
        .price(BigDecimal.valueOf(99.99));
  }

  public static Product.ProductBuilder productWithArticle(String article) {
    return product().article(article);
  }

  // Stock builders
  public static Stock.StockBuilder stock() {
    return Stock.builder().quantity(100);
  }

  public static Stock.StockBuilder stockWithProduct(Product product, StorageLocation location) {
    return stock().product(product).storageLocation(location);
  }

  public static StockId stockId(UUID storageLocationId, UUID productId) {
    return new StockId(storageLocationId, productId);
  }

  // Sale builders
  public static Sale.SaleBuilder sale() {
    return Sale.builder()
        .total(BigDecimal.valueOf(199.99))
        .isReturn(false)
        .soldAt(LocalDateTime.now());
  }

  public static Sale.SaleBuilder saleWithStore(Store store) {
    return sale().store(store);
  }

  public static Sale.SaleBuilder saleWithCashier(Employee cashier) {
    return sale().cashier(cashier);
  }

  public static Sale.SaleBuilder saleComplete(Store store, Employee cashier) {
    return sale().store(store).cashier(cashier);
  }

  public static Sale.SaleBuilder returnSale() {
    return sale().isReturn(true).total(BigDecimal.valueOf(-199.99));
  }

  // SaleItem builders
  public static SaleItem.SaleItemBuilder saleItem() {
    return SaleItem.builder().quantity(1).unitPrice(BigDecimal.valueOf(99.99));
  }

  public static SaleItem.SaleItemBuilder saleItemWithProduct(Product product) {
    return saleItem().product(product).unitPrice(product.getPrice());
  }

  public static SaleItem.SaleItemBuilder saleItemComplete(
      Sale sale, Product product, int quantity) {
    return saleItem().sale(sale).product(product).quantity(quantity).unitPrice(product.getPrice());
  }

  // Timesheet builders
  public static Timesheet.TimesheetBuilder timesheet() {
    return Timesheet.builder()
        .workDate(LocalDate.now())
        .checkIn(LocalTime.of(9, 0))
        .checkOut(LocalTime.of(17, 0))
        .hoursWorked(BigDecimal.valueOf(8.0));
  }

  public static Timesheet.TimesheetBuilder timesheetWithEmployee(Employee employee) {
    return timesheet().employee(employee);
  }

  public static Timesheet.TimesheetBuilder openTimesheet(Employee employee) {
    return timesheet().employee(employee).checkOut(null).hoursWorked(null);
  }

  public static Timesheet.TimesheetBuilder timesheetForDate(Employee employee, LocalDate date) {
    return timesheet().employee(employee).workDate(date);
  }
}
