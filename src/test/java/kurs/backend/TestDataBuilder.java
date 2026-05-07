package kurs.backend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import kurs.backend.domain.persistence.entity.*;

/** Test data builder utility for creating test entities with sensible defaults. */
public class TestDataBuilder {

  public static User.UserBuilder user() {
    return User.builder()
        .id(UUID.randomUUID())
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

  public static Employee.EmployeeBuilder employee() {
    return Employee.builder()
        .id(UUID.randomUUID())
        .fullName("John Doe")
        .position("Cashier")
        .hourlyRate(BigDecimal.valueOf(25.00))
        .hiredAt(LocalDate.now());
  }

  public static Store store() {
    Store store = new Store();
    store.setId(UUID.randomUUID());
    store.setName("Test Store");
    store.setPhone("555-0001");
    store.setIsActive(true);
    return store;
  }

  public static Warehouse warehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.setId(UUID.randomUUID());
    warehouse.setName("Test Warehouse");
    warehouse.setPhone("555-0002");
    warehouse.setIsActive(true);
    return warehouse;
  }

  public static Product.ProductBuilder product() {
    return Product.builder()
        .id(UUID.randomUUID())
        .name("Test Product")
        .article("TEST-SKU-001")
        .price(BigDecimal.valueOf(99.99));
  }

  public static Sale.SaleBuilder sale() {
    return Sale.builder().id(UUID.randomUUID()).total(BigDecimal.valueOf(199.99)).isReturn(false);
  }

  public static SaleItem.SaleItemBuilder saleItem() {
    return SaleItem.builder()
        .id(UUID.randomUUID())
        .quantity(1)
        .unitPrice(BigDecimal.valueOf(99.99));
  }

  public static Stock.StockBuilder stock() {
    return Stock.builder().quantity(100);
  }

  public static Timesheet.TimesheetBuilder timesheet() {
    return Timesheet.builder()
        .id(UUID.randomUUID())
        .workDate(LocalDate.now())
        .checkIn(LocalTime.of(9, 0))
        .checkOut(LocalTime.of(17, 0))
        .hoursWorked(BigDecimal.valueOf(8.0));
  }

  public static Location.LocationBuilder location() {
    return Location.builder().id(UUID.randomUUID()).address("789 Location Blvd");
  }
}
