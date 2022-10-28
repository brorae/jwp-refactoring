package kitchenpos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kitchenpos.common.DatabaseCleaner;
import kitchenpos.dao.MenuDao;
import kitchenpos.dao.MenuGroupDao;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderLineItemDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.ProductDao;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.Product;
import kitchenpos.dto.OrderCreateRequest;
import kitchenpos.dto.OrderLineItemRequest;
import kitchenpos.dto.OrderResponse;
import kitchenpos.dto.OrderUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest extends ServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderTableDao orderTableDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private MenuGroupDao menuGroupDao;

    @Autowired
    private MenuDao menuDao;

    @Autowired
    private OrderLineItemDao orderLineItemDao;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    private Product product;

    private MenuGroup menuGroup;

    private Menu menu;

    @BeforeEach
    void setUp() {
        databaseCleaner.tableClear();

        product = productDao.save(new Product("치킨", BigDecimal.valueOf(10000)));
        menuGroup = menuGroupDao.save(new MenuGroup("1번 메뉴 그룹"));
        menu = menuDao.save(new Menu("1번 메뉴", BigDecimal.valueOf(10000), menuGroup.getId(),
                createMenuProducts(product.getId())));
    }

    @DisplayName("주문을 등록할 수 있다.")
    @Test
    void create() {
        OrderTable newOrderTable = new OrderTable(4, false);
        OrderTable orderTable = orderTableDao.save(newOrderTable);
        OrderCreateRequest request = new OrderCreateRequest(orderTable.getId(),
                createOrderLineItemRequest(menu.getId()));

        OrderResponse response = orderService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo("COOKING");
        assertThat(response.getOrderTableId()).isNotNull();
    }

    @DisplayName("주문 등록 시 주문항목의 메뉴에 등록되어 있지 않은 주문 항목이 있으면 예외가 발생한다.")
    @Test
    void createWithInvalidOrderLineItem() {
        OrderTable newOrderTable = new OrderTable(4, false);
        OrderTable orderTable = orderTableDao.save(newOrderTable);
        OrderCreateRequest request = new OrderCreateRequest(orderTable.getId(),
                createInvalidOrderLineItemRequest());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 등록 시 주문에서의 주문 테이블이 존재하지 않는 주문 테이블일 경우 예외가 발생한다.")
    @Test
    void createWithInvalidOrderTable() {
        OrderCreateRequest request = new OrderCreateRequest(9999L, createOrderLineItemRequest());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 등록 시 주문에서의 주문 테이블이 비어있으면 예외가 발생한다.")
    @Test
    void createWithEmptyOrderTable() {
        OrderTable newOrderTable = new OrderTable(0, true);
        OrderTable orderTable = orderTableDao.save(newOrderTable);
        OrderCreateRequest request = new OrderCreateRequest(orderTable.getId(),
                createOrderLineItemRequest());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문들을 조회할 수 있다.")
    @Test
    void list() {
        OrderTable newOrderTable = new OrderTable(4, false);
        OrderTable orderTable = orderTableDao.save(newOrderTable);
        Order newOrder = new Order(orderTable.getId(), "COOKING", LocalDateTime.now(),
                createOrderLineItem(menu.getId()));
        Order order = orderDao.save(newOrder);
        orderLineItemDao.save(new OrderLineItem(order.getId(), menu.getId(), 10));
        List<OrderResponse> response = orderService.list();

        assertAll(
                () -> assertThat(response.size()).isEqualTo(1L),
                () -> assertThat(response.get(0).getOrderLineItems()).isNotEmpty()
        );
    }

    @DisplayName("주문의 상태를 수정할 수 있다.")
    @Test
    void changeOrderStatus() {
        OrderTable newOrderTable = new OrderTable(4, false);
        OrderTable orderTable = orderTableDao.save(newOrderTable);
        Order newOrder = new Order(orderTable.getId(), "COOKING", LocalDateTime.now(),
                createOrderLineItem(menu.getId()));
        Order order = orderDao.save(newOrder);
        OrderUpdateRequest request = new OrderUpdateRequest("MEAL");

        orderService.changeOrderStatus(order.getId(), request);
        Order foundOrder = orderDao.findById(order.getId()).get();

        assertThat(foundOrder.getOrderStatus()).isEqualTo(OrderStatus.MEAL.name());
    }

    @DisplayName("주문 수정 시 존재하지 않는 주문일 경우 예외가 발생한다.")
    @Test
    void changeOrderStatusWithInvalidOrder() {
        OrderUpdateRequest request = new OrderUpdateRequest("MEAL");

        assertThatThrownBy(() -> orderService.changeOrderStatus(9999L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 수정 시 주문 상태가 계산 완료인 경우 예외가 발생한다.")
    @Test
    void changeOrderStatusWithCompletionOrderStatus() {
        OrderTable newOrderTable = new OrderTable(4, false);
        OrderTable orderTable = orderTableDao.save(newOrderTable);
        Order newOrder = new Order(orderTable.getId(), "COOKING", LocalDateTime.now(),
                createOrderLineItem(menu.getId()));
        Order order = orderDao.save(newOrder);
        OrderUpdateRequest request = new OrderUpdateRequest("COMPLETION");

        orderService.changeOrderStatus(order.getId(), request);

        assertThatThrownBy(() -> orderService.changeOrderStatus(order.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private List<OrderLineItem> createInvalidOrderLineItem() {
        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem(9999L, 10));
        return orderLineItems;
    }

    private List<OrderLineItemRequest> createInvalidOrderLineItemRequest() {
        List<OrderLineItemRequest> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItemRequest(9999L, 10L));
        return orderLineItems;
    }

    private List<OrderLineItem> createOrderLineItem(Long... menuIds) {
        List<OrderLineItem> orderLineItems = new ArrayList<>();
        for (Long menuId : menuIds) {
            orderLineItems.add(new OrderLineItem(menuId, 10));
        }
        return orderLineItems;
    }

    private List<OrderLineItemRequest> createOrderLineItemRequest(Long... menuIds) {
        List<OrderLineItemRequest> orderLineItems = new ArrayList<>();
        for (Long menuId : menuIds) {
            orderLineItems.add(new OrderLineItemRequest(menuId, 10L));
        }
        return orderLineItems;
    }

    private List<MenuProduct> createMenuProducts(Long... productIds) {
        List<MenuProduct> menuProducts = new ArrayList<>();
        for (Long productId : productIds) {
            menuProducts.add(new MenuProduct(productId, 1L, BigDecimal.valueOf(10000)));
        }
        return menuProducts;
    }
}
