package kurs.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.SaleDao;
import kurs.backend.domain.persistence.entity.Employee;
import kurs.backend.domain.persistence.entity.Sale;
import kurs.backend.domain.persistence.entity.SaleItem;

/**
 * Проведение продаж и возвратов.
 *
 * <ul>
 *   <li>CASHIER — создаёт продажу от своего имени (автоматически берём Employee по userId).
 *   <li>ADMIN, MANAGER — могут просматривать продажи.
 *   <li>При продаже Stock в магазине уменьшается через StockService.deduct().
 *   <li>При возврате Stock восстанавливается через StockService.restore().
 * </ul>
 */
public class SaleService {

  private final SaleDao saleDao;
  private final EmployeeDao employeeDao;
  private final StockService stockService;

  public SaleService(SaleDao saleDao, EmployeeDao employeeDao, StockService stockService) {
    this.saleDao = saleDao;
    this.employeeDao = employeeDao;
    this.stockService = stockService;
  }

  // -----------------------------------------------------------------------
  // Read
  // -----------------------------------------------------------------------

  public List<Sale> findByStore(AuthenticatedUser caller, UUID storeId) {
    requireNotGuest(caller);
    return saleDao.findByStoreId(storeId);
  }

  public Sale findById(AuthenticatedUser caller, UUID id) {
    requireNotGuest(caller);
    return saleDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Продажа не найдена", "SALE_NOT_FOUND"));
  }

  // -----------------------------------------------------------------------
  // Create sale
  // -----------------------------------------------------------------------

  /**
   * Проводит продажу.
   *
   * <p>Алгоритм:
   *
   * <ol>
   *   <li>Находим Employee по userId кассира.
   *   <li>Для каждой позиции списываем Stock из магазина кассира.
   *   <li>Считаем итог и сохраняем Sale + SaleItems.
   * </ol>
   *
   * @param items список позиций (у каждой должны быть заполнены product и quantity; unitPrice
   *     берётся из product.price, чтобы исключить подмену цены)
   */
  public Sale processSale(AuthenticatedUser caller, List<SaleItem> items) {
    requireCashierOrAdmin(caller);

    Employee cashier =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Кассир не найден среди сотрудников", "EMPLOYEE_NOT_FOUND"));

    UUID storeId = cashier.getStore().getId();

    // Списываем Stock — если хоть по одному товару не хватает, бросает ServiceException
    for (SaleItem item : items) {
      stockService.deduct(storeId, item.getProduct().getId(), item.getQuantity());
    }

    // Фиксируем цену на момент продажи
    BigDecimal total = BigDecimal.ZERO;
    for (SaleItem item : items) {
      BigDecimal unitPrice = item.getProduct().getPrice();
      item.setUnitPrice(unitPrice);
      total = total.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    Sale sale =
        Sale.builder()
            .store(cashier.getStore())
            .cashier(cashier)
            .total(total)
            .isReturn(false)
            .items(items)
            .build();

    // Устанавливаем обратную ссылку
    items.forEach(i -> i.setSale(sale));

    return saleDao.save(sale);
  }

  /**
   * Оформляет возврат по существующей продаже. Создаёт новую запись Sale с isReturn = true и
   * восстанавливает Stock.
   */
  public Sale processReturn(AuthenticatedUser caller, UUID originalSaleId) {
    requireCashierOrAdmin(caller);

    Sale original =
        saleDao
            .findById(originalSaleId)
            .orElseThrow(
                () -> new ServiceException("Исходная продажа не найдена", "SALE_NOT_FOUND"));

    if (original.getIsReturn()) {
      throw new ServiceException("Нельзя оформить возврат на возврат", "SALE_ALREADY_RETURN");
    }

    Employee cashier =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Кассир не найден среди сотрудников", "EMPLOYEE_NOT_FOUND"));

    UUID storeId = original.getStore().getId();

    // Восстанавливаем Stock
    for (SaleItem item : original.getItems()) {
      stockService.restore(storeId, item.getProduct().getId(), item.getQuantity());
    }

    // Дублируем позиции для новой записи возврата
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

    Sale returnSale =
        Sale.builder()
            .store(original.getStore())
            .cashier(cashier)
            .total(original.getTotal().negate())
            .isReturn(true)
            .items(returnItems)
            .build();

    returnItems.forEach(i -> i.setSale(returnSale));

    return saleDao.save(returnSale);
  }

  // -----------------------------------------------------------------------
  // Guards
  // -----------------------------------------------------------------------

  private void requireCashierOrAdmin(AuthenticatedUser caller) {
    if (!caller.isCashier() && !caller.isAdmin()) {
      throw new AccessDeniedException("Только CASHIER или ADMIN может проводить продажи");
    }
  }

  private void requireNotGuest(AuthenticatedUser caller) {
    if (caller.isGuest()) {
      throw new AccessDeniedException("GUEST не имеет доступа к продажам");
    }
  }
}
