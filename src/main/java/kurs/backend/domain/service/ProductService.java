package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateProductRequest;
import kurs.backend.domain.dto.request.UpdateProductRequest;
import kurs.backend.domain.dto.response.ProductResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.ProductDao;
import kurs.backend.domain.persistence.entity.Product;

/**
 * Управление товарами.
 *
 * <ul>
 *   <li>ADMIN — полный CRUD.
 *   <li>MANAGER — только чтение.
 *   <li>CASHIER — только чтение.
 * </ul>
 */
@AllArgsConstructor
public class ProductService {

  private static final Logger log = LogManager.getLogger(ProductService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  private final ProductDao productDao;

  public List<ProductResponse> findAll(AuthenticatedUser caller) {
    // Все авторизованные пользователи могут просматривать товары
    log.debug("Finding all products: userId={}", caller.getUserId());
    List<ProductResponse> result =
        productDao.findAll().stream().map(ProductResponse::from).toList();
    log.debug("Found {} products", result.size());
    return result;
  }

  public ProductResponse findById(AuthenticatedUser caller, UUID id) {
    log.debug("Finding product by id: productId={}, userId={}", id, caller.getUserId());
    Product product = getOrThrow(id);
    return ProductResponse.from(product);
  }

  public ProductResponse findByArticle(AuthenticatedUser caller, String article) {
    log.debug("Finding product by article: article={}, userId={}", article, caller.getUserId());
    Product product =
        productDao
            .findByArticle(article)
            .orElseThrow(
                () -> {
                  log.warn("Product not found by article: article={}", article);
                  return new ServiceException("Товар не найден", "PRODUCT_NOT_FOUND");
                });
    return ProductResponse.from(product);
  }

  public ProductResponse create(AuthenticatedUser caller, CreateProductRequest req) {
    requireAdmin(caller);
    req.validate();

    log.info(
        "Creating product: name={}, article={}, price={}, userId={}",
        req.getName(),
        req.getArticle(),
        req.getPrice(),
        caller.getUserId());

    // Проверка уникальности артикула
    if (productDao.findByArticle(req.getArticle()).isPresent()) {
      log.warn("Product creation failed: article already exists - article={}", req.getArticle());
      throw new ServiceException("Товар с таким артикулом уже существует", "ARTICLE_EXISTS");
    }

    Product product =
        Product.builder()
            .name(req.getName())
            .article(req.getArticle())
            .price(req.getPrice())
            .build();

    Product saved = productDao.save(product);
    log.info(
        "Product created successfully: productId={}, article={}, price={}",
        saved.getId(),
        saved.getArticle(),
        saved.getPrice());
    auditLog.info(
        "Product created: productId={}, name={}, article={}, price={}, userId={}",
        saved.getId(),
        saved.getName(),
        saved.getArticle(),
        saved.getPrice(),
        caller.getUserId());

    return ProductResponse.from(saved);
  }

  public ProductResponse update(AuthenticatedUser caller, UpdateProductRequest req) {
    requireAdmin(caller);
    req.validate();

    log.info("Updating product: productId={}, userId={}", req.getId(), caller.getUserId());

    Product product = getOrThrow(req.getId());

    if (req.getName() != null && !req.getName().isBlank()) {
      log.debug("Updating product name: productId={}, newName={}", req.getId(), req.getName());
      product.setName(req.getName());
    }

    if (req.getArticle() != null && !req.getArticle().isBlank()) {
      // Проверка уникальности артикула (если изменяется)
      if (!product.getArticle().equals(req.getArticle())) {
        if (productDao.findByArticle(req.getArticle()).isPresent()) {
          log.warn(
              "Product update failed: article already exists - productId={}, article={}",
              req.getId(),
              req.getArticle());
          throw new ServiceException("Товар с таким артикулом уже существует", "ARTICLE_EXISTS");
        }
        log.debug(
            "Updating product article: productId={}, newArticle={}", req.getId(), req.getArticle());
        product.setArticle(req.getArticle());
      }
    }

    if (req.getPrice() != null) {
      log.debug("Updating product price: productId={}, newPrice={}", req.getId(), req.getPrice());
      product.setPrice(req.getPrice());
    }

    Product updated = productDao.update(product);
    log.info("Product updated successfully: productId={}", updated.getId());
    auditLog.info(
        "Product updated: productId={}, name={}, article={}, price={}, userId={}",
        updated.getId(),
        updated.getName(),
        updated.getArticle(),
        updated.getPrice(),
        caller.getUserId());

    return ProductResponse.from(updated);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deleting product: productId={}, userId={}", id, caller.getUserId());
    Product product = getOrThrow(id);
    productDao.delete(product);
    log.info("Product deleted successfully: productId={}", id);
    auditLog.info(
        "Product deleted: productId={}, name={}, article={}, userId={}",
        id,
        product.getName(),
        product.getArticle(),
        caller.getUserId());
  }

  private Product getOrThrow(UUID id) {
    return productDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Товар не найден", "PRODUCT_NOT_FOUND"));
  }

  private void requireAdmin(AuthenticatedUser caller) {
    if (!caller.isAdmin()) {
      throw new AccessDeniedException("Требуется роль ADMIN");
    }
  }
}
