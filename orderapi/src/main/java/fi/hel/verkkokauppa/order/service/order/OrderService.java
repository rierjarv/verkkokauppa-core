package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.data.CustomerDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class OrderService {
        
    private Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMetaService orderItemMetaService;

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;


    public ResponseEntity<OrderAggregateDto> orderAggregateDto(String orderId) {
        OrderAggregateDto orderAggregateDto = getOrderWithItems(orderId);

        if (orderAggregateDto == null) {
            Error error = new Error("order-not-found-from-backend", "order with id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return ResponseEntity.ok().body(orderAggregateDto);
    }

    public OrderAggregateDto getOrderWithItems(final String orderId) {
        OrderAggregateDto orderAggregateDto = null;
        Order order = findById(orderId);

        if (order != null) {
            List<OrderItem> items = this.orderItemService.findByOrderId(orderId);
            List<OrderItemMeta> metas = this.orderItemMetaService.findByOrderId(orderId);
            orderAggregateDto = orderTransformerUtils.transformToOrderAggregateDto(order, items, metas);
        }

        return orderAggregateDto;
    }

    public String generateOrderId(String namespace, String user, String timestamp) {
        String whoseOrder = UUIDGenerator.generateType3UUIDString(namespace, user);
        String orderId = UUIDGenerator.generateType3UUIDString(whoseOrder, timestamp);
        return orderId;
    }

    public Order createByParams(String namespace, String user) {
        String createdAt = DateTimeUtil.getDateTime();
        String orderId = generateOrderId(namespace, user, createdAt);
        Order order = new Order(orderId, namespace, user, createdAt);

        orderRepository.save(order);
        log.debug("created new order, orderId: " + orderId);
        return order;
    }

    public Order findById(String orderId) {
        Optional<Order> mapping = orderRepository.findById(orderId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.warn("order not found, orderId: " + orderId);
        return null;
    }

    public Order findByNamespaceAndUser(String namespace, String user) {
        List<Order> matchingOrders = orderRepository.findByNamespaceAndUser(namespace, user);

        if (matchingOrders.size() > 0)
            return matchingOrders.get(0);

        log.debug("order not found, namespace: " + namespace + " user: " + user);
        return null;
    }


    public void setCustomer(String orderId, CustomerDto customerDto) {
        Order order = findById(orderId);
        if (order != null)
            setCustomer(order, customerDto.getCustomerFirstName(), customerDto.getCustomerLastName(), customerDto.getCustomerEmail(), customerDto.getCustomerPhone());
    }

    public void setCustomer(Order order, CustomerDto customerDto) {
        setCustomer(order, customerDto.getCustomerFirstName(), customerDto.getCustomerLastName(), customerDto.getCustomerEmail(), customerDto.getCustomerPhone());
    }

    public void setCustomer(Order order, String customerFirstName, String customerLastName, String customerEmail, String customerPhone) {
        order.setCustomerFirstName(customerFirstName);
        order.setCustomerLastName(customerLastName);
        order.setCustomerEmail(customerEmail);
        order.setCustomerPhone(customerPhone);

        orderRepository.save(order);
        log.debug("saved order customer details, orderId: " + order.getOrderId());
    }

    public void setTotals(String orderId, String priceNet, String priceVat, String priceTotal) {
        Order order = findById(orderId);
        if (order != null)
            setTotals(order, priceNet, priceVat, priceTotal);        
    }

    public void setTotals(Order order, String priceNet, String priceVat, String priceTotal) {
        order.setPriceNet(priceNet);
        order.setPriceVat(priceVat);
        order.setPriceTotal(priceTotal);

        orderRepository.save(order);
        log.debug("saved order price totals, orderId: " + order.getOrderId());
    }

    public void markAsAccounted(String orderId) {
        Order order = findById(orderId);
        order.setAccounted(DateTimeUtil.getDate());
        orderRepository.save(order);
        log.debug("marked order accounted, orderId: " + order.getOrderId());
    }

    public void setType(Order order, String type) {
        order.setType(type);
        
        orderRepository.save(order);
        log.debug("set order type, orderId: " + order.getOrderId() + " type: " + order.getType());
    }

    public void confirm(Order order ) {
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.debug("confirmed order, orderId: " + order.getOrderId());
    }

    public void cancel(Order order ) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.debug("canceled order, orderId: " + order.getOrderId());
    }

    public Order findByIdValidateByUser(String orderId, String userId) {
        Order order = findById(orderId);

        if (order == null) {
            Error error = new Error("order-not-found-from-backend", "order with id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        String orderUserId = order.getUser();
        if (orderUserId == null || userId == null || !orderUserId.equals(userId)) {
            log.error("unauthorized attempt to load order, userId does not match");
            Error error = new Error("order-not-found-from-backend", "order with order id [" + orderId + "] and user id ["+ userId +"] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return order;
    }
}
