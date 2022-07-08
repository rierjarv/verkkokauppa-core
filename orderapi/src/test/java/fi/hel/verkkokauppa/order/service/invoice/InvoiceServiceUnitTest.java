package fi.hel.verkkokauppa.order.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.invoice.InvoiceDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.mapper.InvoiceMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.invoice.Invoice;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.order.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringJUnit4ClassRunner.class)
@UnitTest
@WebMvcTest(InvoiceService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
class InvoiceServiceUnitTest extends DummyData {

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OrderRepository orderRepository;
    @MockBean
    InvoiceService invoiceService;

    @MockBean
    InvoiceMapper invoiceMapper;

    @MockBean
    OrderTransformerUtils orderTransformerUtils;

    @Test
    void saveInvoiceToOrder() throws NoSuchMethodException {
        Order order = generateDummyOrder();

        // Mocking the method `saveInvoiceToOrder` to call the real method.
        Mockito.when(invoiceService.saveInvoiceToOrder(any(), any())).thenCallRealMethod();

        ReflectionTestUtils.setField(invoiceMapper, "mapper", new ObjectMapper());

        ReflectionTestUtils.setField(invoiceService, "invoiceMapper", invoiceMapper);

        ReflectionTestUtils.setField(invoiceService, "orderRepository", orderRepository);

        ReflectionTestUtils.setField(invoiceMapper, "genericModelTypeObject", new Invoice());

        Mockito.when(invoiceMapper.toDto(any(Invoice.class))).thenAnswer(i -> invoiceMapper.toDto((Invoice) i.getArguments()[0]));
        Mockito.when(invoiceMapper.fromDto(any(InvoiceDto.class))).thenCallRealMethod();

        Mockito.when(orderTransformerUtils.transformToOrderAggregateDto(any(), any(), any())).thenCallRealMethod();

        InvoiceDto invoiceDto = InvoiceDto.builder()
                .orderId(order.getOrderId())
                .userId(order.getUser())
                .businessId("test-businessId")
                .name("test-name")
                .address("test-address")
                .postcode("test-postcode")
                .city("test-city")
                .ovtId("test-ovtId")
                .build();

        // Mocking the `save` method of the `OrderRepository` to return the same object that was passed to it.
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order argument = (Order) i.getArguments()[0];
            argument.setInvoice(invoiceMapper.fromDto(invoiceDto));
            return argument;
        });


        Order createdOrderWithInvoice = invoiceService.saveInvoiceToOrder(invoiceDto, order);

        Assertions.assertEquals(createdOrderWithInvoice.getInvoice().getBusinessId(), "test-businessId");
        Assertions.assertEquals(createdOrderWithInvoice.getInvoice().getName(), "test-name");
        Assertions.assertEquals(createdOrderWithInvoice.getInvoice().getAddress(), "test-address");
        Assertions.assertEquals(createdOrderWithInvoice.getInvoice().getPostcode(), "test-postcode");
        Assertions.assertEquals(createdOrderWithInvoice.getInvoice().getCity(), "test-city");
        Assertions.assertEquals(createdOrderWithInvoice.getInvoice().getOvtId(), "test-ovtId");

    }
}