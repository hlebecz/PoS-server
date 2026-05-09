package kurs.backend.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kurs.backend.TestDataBuilder;
import kurs.backend.domain.dto.request.CreateSaleRequest;
import kurs.backend.domain.dto.request.CreateSaleRequest.SaleItemRequest;
import kurs.backend.domain.dto.response.SaleResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.ProductDao;
import kurs.backend.domain.persistence.dao.SaleDao;
import kurs.backend.domain.persistence.entity.Employee;
import kurs.backend.domain.persistence.entity.Product;
import kurs.backend.domain.persistence.entity.Sale;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

  @Mock private SaleDao saleDao;
  @Mock private EmployeeDao employeeDao;
  @Mock private ProductDao productDao;
  @Mock private StockService stockService;

  private SaleService saleService;

  @BeforeEach
  void setUp() {
    saleService = new SaleService(saleDao, employeeDao, productDao, stockService);
  }

  @Test
  void findByStore_shouldReturnSalesForStore() {
    UUID storeId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Store store = TestDataBuilder.store();
    store.setId(storeId);
    Employee cashier = TestDataBuilder.employee().build();
    Sale sale1 = TestDataBuilder.sale().store(store).cashier(cashier).items(List.of()).build();
    Sale sale2 = TestDataBuilder.sale().store(store).cashier(cashier).items(List.of()).build();

    when(saleDao.findByStoreId(storeId)).thenReturn(List.of(sale1, sale2));

    List<SaleResponse> result = saleService.findByStore(caller, storeId);

    assertEquals(2, result.size());
    verify(saleDao).findByStoreId(storeId);
  }

  @Test
  void findByStore_shouldThrowExceptionForGuest() {
    UUID storeId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("guest")
            .role(UserRole.GUEST)
            .build();

    AccessDeniedException exception =
        assertThrows(AccessDeniedException.class, () -> saleService.findByStore(caller, storeId));

    assertEquals("GUEST не имеет доступа к продажам", exception.getMessage());
  }

  @Test
  void findById_shouldReturnSale() {
    UUID saleId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Store store = TestDataBuilder.store();
    Employee cashier = TestDataBuilder.employee().build();
    Sale sale =
        TestDataBuilder.sale().id(saleId).store(store).cashier(cashier).items(List.of()).build();
    when(saleDao.findById(saleId)).thenReturn(Optional.of(sale));

    SaleResponse result = saleService.findById(caller, saleId);

    assertNotNull(result);
    verify(saleDao).findById(saleId);
  }

  @Test
  void findById_shouldThrowExceptionWhenNotFound() {
    UUID saleId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    when(saleDao.findById(saleId)).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> saleService.findById(caller, saleId));

    assertEquals("SALE_NOT_FOUND", exception.getErrorCode());
    assertEquals("Продажа не найдена", exception.getMessage());
  }

  @Test
  void processSale_shouldCreateSaleSuccessfully() {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Store store = TestDataBuilder.store();
    store.setId(storeId);
    Employee cashier = TestDataBuilder.employee().store(store).build();
    Product product =
        TestDataBuilder.product().id(productId).price(BigDecimal.valueOf(100.00)).build();

    SaleItemRequest itemRequest =
        SaleItemRequest.builder().productId(productId).quantity(2).build();

    CreateSaleRequest request = CreateSaleRequest.builder().items(List.of(itemRequest)).build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.of(cashier));
    when(productDao.findById(productId)).thenReturn(Optional.of(product));

    Sale savedSale = TestDataBuilder.sale().store(store).cashier(cashier).build();
    when(saleDao.save(any(Sale.class))).thenReturn(savedSale);

    SaleResponse result = saleService.processSale(caller, request);

    assertNotNull(result);
    verify(employeeDao).findByUserId(userId);
    verify(productDao).findById(productId);
    verify(stockService).deduct(storeId, productId, 2);
    verify(saleDao).save(any(Sale.class));
  }

  @Test
  void processSale_shouldThrowExceptionForNonCashier() {
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("manager")
            .role(UserRole.MANAGER)
            .build();

    CreateSaleRequest request = CreateSaleRequest.builder().items(List.of()).build();

    AccessDeniedException exception =
        assertThrows(AccessDeniedException.class, () -> saleService.processSale(caller, request));

    assertEquals("Только CASHIER или ADMIN может проводить продажи", exception.getMessage());
  }

  @Test
  void processSale_shouldThrowExceptionWhenEmployeeNotFound() {
    UUID userId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    SaleItemRequest itemRequest =
        SaleItemRequest.builder().productId(productId).quantity(1).build();

    CreateSaleRequest request = CreateSaleRequest.builder().items(List.of(itemRequest)).build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> saleService.processSale(caller, request));

    assertEquals("EMPLOYEE_NOT_FOUND", exception.getErrorCode());
    assertEquals("Кассир не найден среди сотрудников", exception.getMessage());
  }

  @Test
  void processSale_shouldThrowExceptionWhenProductNotFound() {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Store store = TestDataBuilder.store();
    store.setId(storeId);
    Employee cashier = TestDataBuilder.employee().store(store).build();

    SaleItemRequest itemRequest =
        SaleItemRequest.builder().productId(productId).quantity(1).build();

    CreateSaleRequest request = CreateSaleRequest.builder().items(List.of(itemRequest)).build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.of(cashier));
    when(productDao.findById(productId)).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> saleService.processSale(caller, request));

    assertEquals("PRODUCT_NOT_FOUND", exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Товар не найден"));
  }

  @Test
  void processSale_shouldCalculateTotalCorrectly() {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID product1Id = UUID.randomUUID();
    UUID product2Id = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Store store = TestDataBuilder.store();
    store.setId(storeId);
    Employee cashier = TestDataBuilder.employee().store(store).build();
    Product product1 =
        TestDataBuilder.product().id(product1Id).price(BigDecimal.valueOf(50.00)).build();
    Product product2 =
        TestDataBuilder.product().id(product2Id).price(BigDecimal.valueOf(30.00)).build();

    SaleItemRequest item1 = SaleItemRequest.builder().productId(product1Id).quantity(2).build();

    SaleItemRequest item2 = SaleItemRequest.builder().productId(product2Id).quantity(3).build();

    CreateSaleRequest request = CreateSaleRequest.builder().items(List.of(item1, item2)).build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.of(cashier));
    when(productDao.findById(product1Id)).thenReturn(Optional.of(product1));
    when(productDao.findById(product2Id)).thenReturn(Optional.of(product2));

    Sale savedSale = TestDataBuilder.sale().store(store).cashier(cashier).build();
    when(saleDao.save(any(Sale.class))).thenReturn(savedSale);

    saleService.processSale(caller, request);

    ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
    verify(saleDao).save(saleCaptor.capture());

    Sale capturedSale = saleCaptor.getValue();
    // Total should be: (50 * 2) + (30 * 3) = 100 + 90 = 190
    assertEquals(0, BigDecimal.valueOf(190.00).compareTo(capturedSale.getTotal()));
  }

  @Test
  void processReturn_shouldCreateReturnSuccessfully() {
    UUID userId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Store store = TestDataBuilder.store();
    store.setId(storeId);
    Employee cashier = TestDataBuilder.employee().store(store).build();
    Product product = TestDataBuilder.product().id(productId).build();

    Sale originalSale =
        TestDataBuilder.sale().id(saleId).store(store).cashier(cashier).isReturn(false).build();
    originalSale.setItems(
        List.of(
            TestDataBuilder.saleItem().product(product).quantity(2).sale(originalSale).build()));

    when(saleDao.findById(saleId)).thenReturn(Optional.of(originalSale));
    when(employeeDao.findByUserId(userId)).thenReturn(Optional.of(cashier));
    when(saleDao.update(any(Sale.class))).thenReturn(originalSale);

    SaleResponse result = saleService.processReturn(caller, saleId);

    assertNotNull(result);
    verify(saleDao).findById(saleId);
    verify(stockService).restore(storeId, productId, 2);
    verify(saleDao).update(originalSale);
    assertTrue(originalSale.getIsReturn());
  }

  @Test
  void processReturn_shouldThrowExceptionForNonCashier() {
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("manager")
            .role(UserRole.MANAGER)
            .build();

    UUID saleId = UUID.randomUUID();

    AccessDeniedException exception =
        assertThrows(AccessDeniedException.class, () -> saleService.processReturn(caller, saleId));

    assertEquals("Только CASHIER или ADMIN может проводить продажи", exception.getMessage());
  }

  @Test
  void processReturn_shouldThrowExceptionWhenSaleNotFound() {
    UUID userId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    when(saleDao.findById(saleId)).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> saleService.processReturn(caller, saleId));

    assertEquals("SALE_NOT_FOUND", exception.getErrorCode());
    assertEquals("Продажа не найдена", exception.getMessage());
  }

  @Test
  void processReturn_shouldThrowExceptionWhenSaleIsAlreadyReturn() {
    UUID userId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Sale returnSale = TestDataBuilder.sale().id(saleId).isReturn(true).build();

    when(saleDao.findById(saleId)).thenReturn(Optional.of(returnSale));

    ServiceException exception =
        assertThrows(ServiceException.class, () -> saleService.processReturn(caller, saleId));

    assertEquals("SALE_ALREADY_RETURN", exception.getErrorCode());
    assertEquals("Нельзя оформить возврат на возврат", exception.getMessage());
  }

  @Test
  void processReturn_shouldThrowExceptionWhenEmployeeNotFound() {
    UUID userId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();

    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Sale originalSale = TestDataBuilder.sale().id(saleId).isReturn(false).build();

    when(saleDao.findById(saleId)).thenReturn(Optional.of(originalSale));
    when(employeeDao.findByUserId(userId)).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> saleService.processReturn(caller, saleId));

    assertEquals("EMPLOYEE_NOT_FOUND", exception.getErrorCode());
    assertEquals("Кассир не найден среди сотрудников", exception.getMessage());
  }
}
