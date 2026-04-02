package kurs.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateSaleRequest;
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
import kurs.backend.domain.persistence.entity.SaleItem;

/**
 * Проведение продаж и возвратов. CASHIER проводит продажу от своего имени (store берётся из его
 * Employee). При нехватке Stock — ServiceException с кодом STOCK_INSUFFICIENT.
 */
@AllArgsConstructor
public class SaleService {

  private final SaleDao saleDao;
  private final EmployeeDao employeeDao;
  private final ProductDao productDao;
  private final StockService stockService;

  public List<SaleResponse> findByStore(AuthenticatedUser caller, UUID storeId) {
    requireNotGuest(caller);
    return saleDao.findByStoreId(storeId).stream().map(SaleResponse::from).toList();
  }

  public SaleResponse findById(AuthenticatedUser caller, UUID id) {
    requireNotGuest(caller);
    return SaleResponse.from(getOrThrow(id));
  }

  public SaleResponse processSale(AuthenticatedUser caller, CreateSaleRequest req) {
    requireCashierOrAdmin(caller);
    req.validate();

    Employee cashier =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Кассир не найден среди сотрудников", "EMPLOYEE_NOT_FOUND"));

    UUID storeId = cashier.getStore().getId();

    // Резолвим продукты и проверяем их наличие до создания записи
    List<SaleItem> items =
        req.getItems().stream()
            .map(
                itemReq -> {
                  Product product =
                      productDao
                          .findById(itemReq.getProductId())
                          .orElseThrow(
                              () ->
                                  new ServiceException(
                                      "Товар не найден: " + itemReq.getProductId(),
                                      "PRODUCT_NOT_FOUND"));
                  return SaleItem.builder()
                      .product(product)
                      .quantity(itemReq.getQuantity())
                      .unitPrice(product.getPrice())
                      .build();
                })
            .toList();

    // Списываем Stock — если хоть по одному товару не хватает, бросает ServiceException
    items.forEach(
        item -> stockService.deduct(storeId, item.getProduct().getId(), item.getQuantity()));

    BigDecimal total =
        items.stream()
            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Sale sale =
        Sale.builder()
            .store(cashier.getStore())
            .cashier(cashier)
            .total(total)
            .isReturn(false)
            .items(items)
            .build();
    items.forEach(i -> i.setSale(sale));

    return SaleResponse.from(saleDao.save(sale));
  }

  /**
   * Оформляет возврат по исходной продаже. Создаёт новую запись Sale с isReturn=true,
   * восстанавливает Stock.
   */
  public SaleResponse processReturn(AuthenticatedUser caller, UUID originalSaleId) {
    requireCashierOrAdmin(caller);

    Sale original = getOrThrow(originalSaleId);
    if (original.getIsReturn())
      throw new ServiceException("Нельзя оформить возврат на возврат", "SALE_ALREADY_RETURN");

    Employee cashier =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Кассир не найден среди сотрудников", "EMPLOYEE_NOT_FOUND"));

    UUID storeId = original.getStore().getId();
    original
        .getItems()
        .forEach(
            item -> stockService.restore(storeId, item.getProduct().getId(), item.getQuantity()));

    List<SaleItem> returnItems =
        original.getItems().stream()
            .map(
                i ->
                    SaleItem.builder()
                        .product(i.getProduct())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
            .toList();

    original.setIsReturn(true);

    return SaleResponse.from(saleDao.update(original));
  }

  private Sale getOrThrow(UUID id) {
    return saleDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Продажа не найдена", "SALE_NOT_FOUND"));
  }

  private void requireCashierOrAdmin(AuthenticatedUser caller) {
    if (!caller.isCashier() && !caller.isAdmin())
      throw new AccessDeniedException("Только CASHIER или ADMIN может проводить продажи");
  }

  private void requireNotGuest(AuthenticatedUser caller) {
    if (caller.isGuest()) throw new AccessDeniedException("GUEST не имеет доступа к продажам");
  }
}
