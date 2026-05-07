package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

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

  private final ProductDao productDao;

  public List<ProductResponse> findAll(AuthenticatedUser caller) {
    // Все авторизованные пользователи могут просматривать товары
    return productDao.findAll().stream().map(ProductResponse::from).toList();
  }

  public ProductResponse findById(AuthenticatedUser caller, UUID id) {
    Product product = getOrThrow(id);
    return ProductResponse.from(product);
  }

  public ProductResponse findByArticle(AuthenticatedUser caller, String article) {
    Product product =
        productDao
            .findByArticle(article)
            .orElseThrow(() -> new ServiceException("Товар не найден", "PRODUCT_NOT_FOUND"));
    return ProductResponse.from(product);
  }

  public ProductResponse create(AuthenticatedUser caller, CreateProductRequest req) {
    requireAdmin(caller);
    req.validate();

    // Проверка уникальности артикула
    if (productDao.findByArticle(req.getArticle()).isPresent()) {
      throw new ServiceException("Товар с таким артикулом уже существует", "ARTICLE_EXISTS");
    }

    Product product =
        Product.builder()
            .name(req.getName())
            .article(req.getArticle())
            .price(req.getPrice())
            .build();

    return ProductResponse.from(productDao.save(product));
  }

  public ProductResponse update(AuthenticatedUser caller, UpdateProductRequest req) {
    requireAdmin(caller);
    req.validate();

    Product product = getOrThrow(req.getId());

    if (req.getName() != null && !req.getName().isBlank()) {
      product.setName(req.getName());
    }

    if (req.getArticle() != null && !req.getArticle().isBlank()) {
      // Проверка уникальности артикула (если изменяется)
      if (!product.getArticle().equals(req.getArticle())) {
        if (productDao.findByArticle(req.getArticle()).isPresent()) {
          throw new ServiceException("Товар с таким артикулом уже существует", "ARTICLE_EXISTS");
        }
        product.setArticle(req.getArticle());
      }
    }

    if (req.getPrice() != null) {
      product.setPrice(req.getPrice());
    }

    return ProductResponse.from(productDao.update(product));
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    Product product = getOrThrow(id);
    productDao.delete(product);
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
