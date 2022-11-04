package kitchenpos.menu.domain.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kitchenpos.menu.dao.MenuDao;
import kitchenpos.menu.dao.MenuProductDao;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.domain.MenuProducts;
import kitchenpos.product.dao.ProductDao;
import kitchenpos.product.domain.Product;
import org.springframework.stereotype.Repository;

@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuDao menuDao;

    private final MenuProductDao menuProductDao;

    private final ProductDao productDao;

    public MenuRepositoryImpl(final MenuDao menuDao, final MenuProductDao menuProductDao, final ProductDao productDao) {
        this.menuDao = menuDao;
        this.menuProductDao = menuProductDao;
        this.productDao = productDao;
    }

    @Override
    public Menu save(final Menu entity) {
        final Menu menu = menuDao.save(entity);
        final List<MenuProduct> savedMenuProducts = new ArrayList<>();
        for (final MenuProduct menuProduct : entity.getMenuProducts()) {
            final MenuProduct savedMenuProduct = menuProductDao.save(
                    new MenuProduct(
                            menuProduct.getSeq(),
                            menu.getId(),
                            menuProduct.getProductId(),
                            menuProduct.getQuantity()
                    ));
            savedMenuProducts.add(
                    new MenuProduct(
                            savedMenuProduct.getSeq(),
                            savedMenuProduct.getMenuId(),
                            savedMenuProduct.getProductId(),
                            savedMenuProduct.getQuantity(),
                            getById(menuProduct.getProductId()).getPrice()
                    )
            );
        }
        return new Menu(menu.getId(), menu.getName(), menu.getPrice(), menu.getMenuGroupId(),
                new MenuProducts(savedMenuProducts, menu.getPrice()));
    }

    @Override
    public Optional<Menu> findById(final Long id) {
        final Optional<Menu> menu = menuDao.findById(id);
        return menu.map(it -> it.setMenuProducts(new MenuProducts(getByMenuId(id), it.getPrice())));
    }

    @Override
    public List<Menu> findAll() {
        final List<Menu> menus = menuDao.findAll();
        return menus.stream()
                .map(menu -> menu.setMenuProducts(new MenuProducts(getByMenuId(menu.getId()), menu.getPrice())))
                .collect(Collectors.toList());
    }

    @Override
    public long countByIdIn(List<Long> ids) {
        return menuDao.countByIdIn(ids);
    }

    private Product getById(Long productId) {
        return productDao.findById(productId)
                .orElseThrow(IllegalArgumentException::new);
    }

    private List<MenuProduct> getByMenuId(final Long menuId) {
        final List<MenuProduct> menuProducts = menuProductDao.findAllByMenuId(menuId);
        return menuProducts.stream()
                .map(it -> new MenuProduct(
                                it.getSeq(),
                                it.getMenuId(),
                                it.getProductId(),
                                it.getQuantity(),
                                getById(it.getProductId()).getPrice()
                        )
                )
                .collect(Collectors.toList());
    }
}
