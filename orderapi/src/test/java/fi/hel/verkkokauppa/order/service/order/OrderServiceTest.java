package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.admin.SubscriptionAdminController;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class OrderServiceTest extends TestUtils {

    private Logger log = LoggerFactory.getLogger(OrderServiceTest.class);
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderController orderController;
    @Autowired
    private NextDateCalculator nextDateCalculator;

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private CreateOrderFromSubscriptionCommand createOrderFromSubscriptionCommand;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private SubscriptionRenewalService subscriptionRenewalService;
    @Autowired
    private SubscriptionAdminController subscriptionAdminController;

    private Order foundOrder;
    private Subscription foundSubscription;

    @Test
    public void assertTrue() {
        Assertions.assertTrue(true);
    }

    @Test
    @RunIfProfile(profile = "local")
    void setOrderStartAndEndDate() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());
        PaymentMessage message = new PaymentMessage();
        String paymentPaidTimestamp = DateTimeUtil.getDateTime();
        message.setPaymentPaidTimestamp(paymentPaidTimestamp);
        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, message);

            Assertions.assertEquals(foundOrder.getStartDate(), DateTimeUtil.fromFormattedDateTimeString(paymentPaidTimestamp));
            // End date = startDate plus periodFrequency and period eq. daily/monthly/yearly.
            Assertions.assertEquals(foundOrder.getEndDate(), DateTimeUtil.fromFormattedDateTimeString(paymentPaidTimestamp).plus(1, ChronoUnit.DAYS));
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    void cancelOrder() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        order.ifPresent(value -> orderService.cancel(value));
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionTested() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        log.info("todayDateAsString {}", todayDateAsString);


        LocalDateTime minusOneDay = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS);

        LocalDateTime real = minusOneDay.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
        // End datetime: start datetime + 1 month - 1day(end of the day)
        String oneMonthFromTodayMinusOneDayEndOfThatDay = real
                .format(formatter);
        log.info("oneMonthFromTodayMinusOneDayEndOfThatDay {}", oneMonthFromTodayMinusOneDayEndOfThatDay);
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        LocalDateTime periodAdded = nextDateCalculator.calculateNextDateTime(
                today,
                orderItemOrder1.getPeriodUnit(),
                orderItemOrder1.getPeriodFrequency());
        LocalDateTime calculatedNewEndDate = nextDateCalculator.calculateNextEndDateTime(
                periodAdded,
                Period.MONTHLY
        );
        Assertions.assertEquals(calculatedNewEndDate.format(formatter),
                oneMonthFromTodayMinusOneDayEndOfThatDay);
        // Asserts that endDate is moved + 1 month from today date - 1 day (end of day).
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription
        Subscription firstSubscription = subscriptionService.findById(firstSubscriptionId);

        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH");

        // Assert dates matches
        Assertions.assertEquals(order1.getStartDate().format(formatter2), firstSubscription.getStartDate().format(formatter2));
        Assertions.assertEquals(order1.getEndDate().format(formatter), firstSubscription.getEndDate().format(formatter));
        log.info("order 1 startDate {}", order1.getStartDate().format(formatter));
        log.info("order 1 endDate {}", order1.getEndDate().format(formatter));
        log.info("subscription 1 startDate {}", firstSubscription.getStartDate().format(formatter));
        log.info("subscription 1 endDate {}", firstSubscription.getEndDate().format(formatter));

        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        Assertions.assertEquals(today.format(formatter2), firstSubscription.getStartDate().format(formatter2));
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, firstSubscription.getEndDate().format(formatter));

        // RENEWAL PROCESS START 1
        // There should be no need to renew this subscription yet
        // next renewal date should be 3 days from endDate (31.12.2021) -> threeDaysBeforeEndDate
        String threeDaysBeforeEndDate = firstSubscription.getEndDate().minus(3, ChronoUnit.DAYS).format(formatter);
        log.info("threeDaysBeforeEndDate {}", threeDaysBeforeEndDate);
        LocalDateTime plusTwentySevenDays = today.plus(27, ChronoUnit.DAYS);
        LocalDateTime endOfDayPlusTwentySevenDays = plusTwentySevenDays.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
        String twentySevenDaysFromTodayEndOfDay = endOfDayPlusTwentySevenDays.format(formatter);
        Assertions.assertEquals(twentySevenDaysFromTodayEndOfDay, threeDaysBeforeEndDate);
        // Renew subscription

        String order2FromSubscriptionId = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        subscriptionRenewalService.finishRenewingSubscription(firstSubscriptionId);
        // Fetch second subs
        Order order2 = orderService.findById(order2FromSubscriptionId);

        // Start datetime: Previous order enddate + 1 day(start of the day)
        LocalDateTime renewalStartDate = order2
                .getEndDate()
                .plus(1, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.MONTHS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
        Assertions.assertEquals(renewalStartDate.format(formatter), order2.getStartDate().format(formatter));

        String twoMonthFromTodayMinusOneDayEndOfThatDay = today
                .plus(2, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
                .format(formatter);
        Assertions.assertEquals(twoMonthFromTodayMinusOneDayEndOfThatDay, order2.getEndDate().format(formatter));
        // RENEWAL PROCESS END 1

        // RENEWAL PROCESS START 2
        log.info("order2.getStartDate() {}", order2.getStartDate().format(formatter));
        log.info("order2.getEndDate() {}", order2.getEndDate().format(formatter));

        // There should be no need to renew this subscription yet, has active subscription order
        String order3FromOrder = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        log.info("order2FromSubscriptionId {}", order2FromSubscriptionId);
        log.info("order3FromOrder {}", order3FromOrder);
        Assertions.assertEquals(order2FromSubscriptionId, order3FromOrder);
        // RENEWAL PROCESS END 2

        // RENEWAL PROCESS START 3
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        firstSubscription.setEndDate(today.minus(1, ChronoUnit.MONTHS));
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription.isPresent()) {
            firstSubscription = refetchFirstSubscription.get();
            Assertions.assertEquals(SubscriptionStatus.CANCELLED, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 3
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionDaily() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        log.info("todayDateAsString {}", todayDateAsString);
        String oneMonthFromMinusOneDayToday = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .format(formatter);
        log.info("oneMonthFromMinusOneDayToday {}", oneMonthFromMinusOneDayToday);
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        String oneMonthFromToday = today
                .plus(1, ChronoUnit.MONTHS)
                .format(formatter);

        Assertions.assertEquals(nextDateCalculator.calculateNextDateTime(
                        today,
                        orderItemOrder1.getPeriodUnit(),
                        orderItemOrder1.getPeriodFrequency()).format(formatter),
                oneMonthFromToday);
        // Asserts that endDate is moved + 1 month minus one day from today date.
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription
        Subscription firstSubscription = subscriptionService.findById(firstSubscriptionId);

        // Assert dates matches
        Assertions.assertEquals(order1.getStartDate().format(formatter), firstSubscription.getStartDate().format(formatter));
        Assertions.assertEquals(order1.getEndDate().format(formatter), firstSubscription.getEndDate().format(formatter));
        log.info("order 1 startDate {}", order1.getStartDate().format(formatter));
        log.info("order 1 endDate {}", order1.getEndDate().format(formatter));

        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        Assertions.assertEquals(todayDateAsString, firstSubscription.getStartDate().format(formatter));
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, order1.getEndDate().format(formatter));
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, firstSubscription.getEndDate().format(formatter));

        // RENEWAL PROCESS START 1
        // There should be no need to renew this subscription yet
        // next renewal date should be 3 days from endDate (31.12.2021) -> threeDaysBeforeEndDate
        String threeDaysBeforeEndDate = firstSubscription.getEndDate().minus(3, ChronoUnit.DAYS).format(formatter);
        log.info("threeDaysBeforeEndDate {}", threeDaysBeforeEndDate);
        String twentySevenDaysFromToday = today.plus(27, ChronoUnit.DAYS).format(formatter);
        Assertions.assertEquals(twentySevenDaysFromToday, threeDaysBeforeEndDate);
        // Renew subscription

        String order2FromSubscriptionId = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        subscriptionRenewalService.finishRenewingSubscription(firstSubscriptionId);
        // Fetch second subs
        Order order2 = orderService.findById(order2FromSubscriptionId);

        Assertions.assertEquals(oneMonthFromToday, order2.getStartDate().format(formatter));

        String twoMonthFromTodayMinusOneDay = today
                .plus(2, ChronoUnit.MONTHS)
                .minus(1,ChronoUnit.DAYS)
                .format(formatter);
        Assertions.assertEquals(twoMonthFromTodayMinusOneDay, order2.getEndDate().format(formatter));
        // RENEWAL PROCESS END 1

        // RENEWAL PROCESS START 2
        log.info("order2.getStartDate() {}", order2.getStartDate().format(formatter));
        log.info("order2.getEndDate() {}", order2.getEndDate().format(formatter));

        // There should be no need to renew this subscription yet, has active subscription order
        String order3FromOrder = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        log.info("order2FromSubscriptionId {}", order2FromSubscriptionId);
        log.info("order3FromOrder {}", order3FromOrder);
        Assertions.assertEquals(order2FromSubscriptionId, order3FromOrder);
        // RENEWAL PROCESS END 2

        // RENEWAL PROCESS START 3
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        firstSubscription.setEndDate(today.minus(1, ChronoUnit.MONTHS));
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription.isPresent()) {
            firstSubscription = refetchFirstSubscription.get();
            Assertions.assertEquals(SubscriptionStatus.CANCELLED, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 3
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionAllowCurrentDayRenewalTested() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        log.info("todayDateAsString {}", todayDateAsString);


        LocalDateTime minusOneDay = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS);

        LocalDateTime real = minusOneDay.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
        // End datetime: start datetime + 1 month - 1day(end of the day)
        String oneMonthFromTodayMinusOneDayEndOfThatDay = real
                .format(formatter);
        log.info("oneMonthFromTodayMinusOneDayEndOfThatDay {}", oneMonthFromTodayMinusOneDayEndOfThatDay);
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        LocalDateTime periodAdded = nextDateCalculator.calculateNextDateTime(
                today,
                orderItemOrder1.getPeriodUnit(),
                orderItemOrder1.getPeriodFrequency());
        LocalDateTime calculatedNewEndDate = nextDateCalculator.calculateNextEndDateTime(
                periodAdded,
                Period.MONTHLY
        );
        Assertions.assertEquals(calculatedNewEndDate.format(formatter),
                oneMonthFromTodayMinusOneDayEndOfThatDay);
        // Asserts that endDate is moved + 1 month from today date - 1 day (end of day).
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription
        Subscription firstSubscription = subscriptionService.findById(firstSubscriptionId);

        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH");

        // Assert dates matches
        Assertions.assertEquals(order1.getStartDate().format(formatter2), firstSubscription.getStartDate().format(formatter2));
        Assertions.assertEquals(order1.getEndDate().format(formatter), firstSubscription.getEndDate().format(formatter));
        log.info("order 1 startDate {}", order1.getStartDate().format(formatter));
        log.info("order 1 endDate {}", order1.getEndDate().format(formatter));
        log.info("subscription 1 startDate {}", firstSubscription.getStartDate().format(formatter));
        log.info("subscription 1 endDate {}", firstSubscription.getEndDate().format(formatter));

        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        Assertions.assertEquals(today.format(formatter2), firstSubscription.getStartDate().format(formatter2));
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, firstSubscription.getEndDate().format(formatter));

        // RENEWAL PROCESS START 1
        // There should be no need to renew this subscription yet
        // next renewal date should be 3 days from endDate (31.12.2021) -> threeDaysBeforeEndDate
        String threeDaysBeforeEndDate = firstSubscription.getEndDate().minus(3, ChronoUnit.DAYS).format(formatter);
        log.info("threeDaysBeforeEndDate {}", threeDaysBeforeEndDate);
        LocalDateTime plusTwentySevenDays = today.plus(27, ChronoUnit.DAYS);
        LocalDateTime endOfDayPlusTwentySevenDays = plusTwentySevenDays.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
        String twentySevenDaysFromTodayEndOfDay = endOfDayPlusTwentySevenDays.format(formatter);
        Assertions.assertEquals(twentySevenDaysFromTodayEndOfDay, threeDaysBeforeEndDate);
        // Renew subscription

        String order2FromSubscriptionId = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        subscriptionRenewalService.finishRenewingSubscription(firstSubscriptionId);
        // Fetch second subs
        Order order2 = orderService.findById(order2FromSubscriptionId);

        // Start datetime: Previous order enddate + 1 day(start of the day)
        LocalDateTime renewalStartDate = order2
                .getEndDate()
                .plus(1, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.MONTHS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
        Assertions.assertEquals(renewalStartDate.format(formatter), order2.getStartDate().format(formatter));

        String twoMonthFromTodayMinusOneDayEndOfThatDay = today
                .plus(2, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
                .format(formatter);
        Assertions.assertEquals(twoMonthFromTodayMinusOneDayEndOfThatDay, order2.getEndDate().format(formatter));
        // RENEWAL PROCESS END 1

        // RENEWAL PROCESS START 2
        log.info("order2.getStartDate() {}", order2.getStartDate().format(formatter));
        log.info("order2.getEndDate() {}", order2.getEndDate().format(formatter));

        // RENEWAL PROCESS START 3 (KYV-505)
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        // Set endDate at the start of the day, '00:00'.
        firstSubscription.setEndDate(
                today.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay())
        );
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription.isPresent()) {
            firstSubscription = refetchFirstSubscription.get();
            Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 3

        // RENEWAL PROCESS START 4 (Automatically cancels subscription when endDate is today -1 day)
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        // Set endDate at the start of the day, '00:00'.
        firstSubscription.setEndDate(
                today
                        .minus(1, ChronoUnit.DAYS)
                        .with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
        );
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription2 = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription2.isPresent()) {
            firstSubscription = refetchFirstSubscription2.get();
            Assertions.assertEquals(SubscriptionStatus.CANCELLED, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 4
    }


}