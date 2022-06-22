package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.logic.fetcher.CancelPaymentFetcher;
import fi.hel.verkkokauppa.payment.logic.validation.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.model.PaymentMethod;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentFilterService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodService;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Objects;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * This class is used to test the controller layer of the application
 * <p>
 * Change PaymentAdminController.class to controller which you want to test.
 */
@WebMvcTest(PaymentAdminController.class) // Change and uncomment
@Import(PaymentAdminController.class) // Change and uncomment
@ContextConfiguration(classes = {AutoMockBeanFactory.class, ValidationAutoConfiguration.class}) // This automatically mocks missing beans
@AutoConfigureMockMvc // This activates auto configuration to call mocked api endpoints.
@Slf4j
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class PaymentAdminControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PaymentAdminController paymentAdminController;

    // You need to add all dependencies in controller with @Autowired annotation
    // as new field with @MockBean to controller test.
    @MockBean
    private PaymentMethodService paymentMethodService;
    @MockBean
    private PaymentMethodRepository paymentMethodRepository;
    @MockBean
    private OnlinePaymentService onlinePaymentService;
    @MockBean
    private CancelPaymentFetcher cancelPaymentFetcher;
    @MockBean
    private PaymentReturnValidator paymentReturnValidator;
    @MockBean
    private SaveHistoryService saveHistoryService;
    @MockBean
    private PaymentFilterService filterService;


    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paymentMethodService, "paymentMethodRepository", paymentMethodRepository);
        ReflectionTestUtils.setField(paymentMethodService, "mapper", mapper);
    }


    @Test
    public void savePaymentFilter() throws Exception {
        List<PaymentFilterDto> request = new ArrayList<>();
        PaymentFilterDto paymentFilterDto = new PaymentFilterDto();
        paymentFilterDto.setReferenceId("setReferenceId");
        paymentFilterDto.setFilterType("setFilterType");
        paymentFilterDto.setValue("setValue");
        request.add(paymentFilterDto);

        List<PaymentFilter> responseFilters = new ArrayList<>();
        PaymentFilter paymentFilter = mapper.convertValue(paymentFilterDto, PaymentFilter.class);
        String filterId = "123";
        paymentFilter.setFilterId(filterId);
        responseFilters.add(paymentFilter);
        when(filterService.savePaymentFilters(any())).thenReturn(responseFilters);

        ReflectionTestUtils.setField(filterService, "objectMapper", mapper);
        when(filterService.mapPaymentFilterListToDtoList(any())).thenCallRealMethod();

        ResponseEntity<List<PaymentFilterDto>> response = paymentAdminController.savePaymentFilters(request);

        Assertions.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        PaymentFilterDto expected = Objects.requireNonNull(response.getBody()).get(0);
        Assertions.assertNotNull(expected.getFilterId());
        Assertions.assertEquals(expected.getReferenceId(), paymentFilterDto.getReferenceId());
        Assertions.assertEquals(expected.getFilterType(), paymentFilterDto.getFilterType());
        Assertions.assertEquals(expected.getValue(), paymentFilterDto.getValue());
    }


    /**
     * It tests the create payment method endpoint.
     */
    @Test
    public void whenCreatePaymentMethodWithValidData_thenReturnStatus201() throws Exception {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        PaymentMethod paymentMethod = mapper.convertValue(paymentMethodDto, PaymentMethod.class);

        Mockito.when(paymentMethodService.createNewPaymentMethod(paymentMethodDto)).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.save(paymentMethod)).thenReturn(paymentMethod);

        MvcResult response = this.mockMvc.perform(
                        post("/payment-admin/payment-method")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(getFileContents("data/paymentMethodCreateRequest.json"))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();

        // TODO: Fix below request body verification - body is empty for even if status is 201 (?)
        /*
        PaymentMethodDto responseDto = objectMapper.readValue(response.getResponse().getContentAsString(), PaymentMethodDto.class);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(responseDto, paymentMethodDto);
        */
    }

    @Test
    public void whenCreatePaymentMethodWithInvalidGatewayData_thenReturnStatus400() throws Exception {
        this.mockMvc.perform(
                        post("/payment-admin/payment-method")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContents("data/paymentMethodCreateInvalidRequest.json"))
                )
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(400));
    }


    @Test
    public void whenCreatePaymentMethodWithSameCodeThatExists_thenReturnError409() {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);
        PaymentMethod paymentMethod = mapper.convertValue(paymentMethodDto, PaymentMethod.class);

        Mockito.when(paymentMethodService.createNewPaymentMethod(paymentMethodDto)).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findByCode(paymentMethodDto.getCode())).thenReturn(Arrays.asList(paymentMethod));


        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            post("/payment-admin/payment-method")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(paymentMethodDto))
                    )
                    .andDo(print())
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(409));

        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());

        assertEquals("payment-method-already-exists", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] already exists", cause.getErrors().getErrors().get(0).getMessage());
    }

    /**
     * It tests the update payment method endpoint.
     */
    @Test
    public void whenUpdatePaymentMethodWithValidData_thenReturnStatus200() throws Exception {
        String initialCode = "test-payment-code";
        PaymentMethodDto initialPaymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        PaymentMethodDto updatedPaymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);
        updatedPaymentMethodDto.setName("Edited test payment method");
        updatedPaymentMethodDto.setCode("test-edit-payment-code");
        updatedPaymentMethodDto.setGroup("test-edit-payment-group");

        PaymentMethod initialPaymentMethod = mapper.convertValue(initialPaymentMethodDto, PaymentMethod.class);
        PaymentMethod updatedPaymentMethod = mapper.convertValue(updatedPaymentMethodDto, PaymentMethod.class);

        Mockito.when(paymentMethodService.updatePaymentMethod(initialCode, updatedPaymentMethodDto)).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findByCode(initialCode)).thenReturn(Arrays.asList(initialPaymentMethod));
        Mockito.when(paymentMethodRepository.save(updatedPaymentMethod)).thenReturn(updatedPaymentMethod);

        MvcResult response = this.mockMvc.perform(
                        put("/payment-admin/payment-method/" + initialCode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContents("data/paymentMethodEditRequest.json"))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();
        // TODO: Fix below request body verification - body is empty for even if status is 201 (?)
        /*
        PaymentMethodDto responseDto = objectMapper.readValue(response.getResponse().getContentAsString(), PaymentMethodDto.class);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(responseDto, updatedPaymentMethodDto);
        */
    }

    @Test
    public void whenUpdatePaymentMethodWithInvalidGatewayData_thenReturnStatus400() throws Exception {
        String code = "test-payment-code";
        this.mockMvc.perform(
                        put("/payment-admin/payment-method/" + code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContents("data/paymentMethodCreateInvalidRequest.json"))
                )
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(400));
    }


    @Test
    public void whenUpdatePaymentMethodThatDoesNotExist_thenReturnError404() {
        String initialCode = "test-payment-code";
        PaymentMethodDto updatedPaymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);
        updatedPaymentMethodDto.setName("Edited test payment method");
        updatedPaymentMethodDto.setCode("test-edit-payment-code");
        updatedPaymentMethodDto.setGroup("test-edit-payment-group");

        Mockito.when(paymentMethodService.updatePaymentMethod(initialCode, updatedPaymentMethodDto)).thenCallRealMethod();

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            put("/payment-admin/payment-method/" + initialCode)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(updatedPaymentMethodDto))
                    )
                    .andDo(print())
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(404));

        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals("payment-method-not-found", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] not found", cause.getErrors().getErrors().get(0).getMessage());
    }

    /**
     * It tests the delete payment method endpoint.
     */
    @Test
    public void whenDeletePaymentMethodThatExists_thenReturnStatus200() throws Exception {
        String code = "test-payment-code";

        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        PaymentMethod paymentMethod = mapper.convertValue(paymentMethodDto, PaymentMethod.class);

        Mockito.when(paymentMethodService.deletePaymentMethod(code)).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findByCode(code)).thenReturn(Arrays.asList(paymentMethod));

        MvcResult response = this.mockMvc.perform(
                        delete("/payment-admin/payment-method/" + code)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();

        String contentAsString = response.getResponse().getContentAsString();
        assertEquals(contentAsString, code);
    }

    @Test
    public void whenDeletePaymentMethodThatDoesNotExist_thenReturnError404() {
        String code = "test-payment-code";

        Mockito.when(paymentMethodService.deletePaymentMethod(code)).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findByCode(code)).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            delete("/payment-admin/payment-method/" + code)
                                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    )
                    .andDo(print())
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(404));

        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals("payment-method-not-found", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] not found", cause.getErrors().getErrors().get(0).getMessage());
    }

    /*
     * It tests the get payment method by code endpoint.
     */
    @Test
    public void whenGetPaymentMethodByCodeThatDoesNotExist_thenReturnStatus200() throws Exception {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        PaymentMethod paymentMethod = mapper.convertValue(paymentMethodDto, PaymentMethod.class);

        Mockito.when(paymentMethodService.getPaymenMethodByCode(paymentMethodDto.getCode())).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findByCode(paymentMethodDto.getCode())).thenReturn(Arrays.asList(paymentMethod));

        MvcResult response = this.mockMvc.perform(
                        get("/payment-admin/payment-method/" + paymentMethodDto.getCode())
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();
        PaymentMethodDto responseDto = mapper.readValue(response.getResponse().getContentAsString(), PaymentMethodDto.class);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(responseDto, paymentMethodDto);
    }

    @Test
    public void whenGetPaymentMethodByCodeThatDoesNotExist_thenReturnError404() {
        String code = "test-payment-code";

        Mockito.when(paymentMethodService.getPaymenMethodByCode(code)).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findByCode(code)).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            get("/payment-admin/payment-method/" + code)
                    )
                    .andDo(print())
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(404));
        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals("payment-method-not-found", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] not found", cause.getErrors().getErrors().get(0).getMessage());
    }

    /*
     * It tests the get all payment methods endpoint.
     */
    @Test
    public void whenGetPaymentMethods_thenReturnZeroWithStatus200() throws Exception {
        Mockito.when(paymentMethodService.getAllPaymentMethods()).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findAll()).thenReturn(Collections.emptyList());

        MvcResult response = this.mockMvc.perform(
                        get("/payment-admin/payment-method/")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();
        String contentAsString = response.getResponse().getContentAsString();
        assertEquals(contentAsString, "[]");
    }

    @Test
    public void whenGetPaymentMethods_thenReturnMoreThanZeroWithStatus200() throws Exception {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        PaymentMethodDto paymentMethodDto2 = createTestPaymentMethodDto(GatewayEnum.ONLINE);
        paymentMethodDto2.setCode("second-payment-code");

        PaymentMethod paymentMethod = mapper.convertValue(paymentMethodDto, PaymentMethod.class);
        PaymentMethod paymentMethod2 = mapper.convertValue(paymentMethodDto2, PaymentMethod.class);

        Mockito.when(paymentMethodService.getAllPaymentMethods()).thenCallRealMethod();
        Mockito.when(paymentMethodRepository.findAll()).thenReturn(Arrays.asList(paymentMethod, paymentMethod2));

        MvcResult response = this.mockMvc.perform(
                        get("/payment-admin/payment-method/")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();

        List<PaymentMethodDto> responseDtos = Arrays.asList(mapper.readValue(response.getResponse().getContentAsString(), PaymentMethodDto[].class));
        Assertions.assertEquals(2, responseDtos.size());
    }



    private PaymentMethodDto createTestPaymentMethodDto(GatewayEnum gateway) {
        return new PaymentMethodDto("Test payment method",
                "test-payment-code",
                "test-payment-group",
                "test-payment.jpg",
                gateway);
    }

    private static String getFileContents(String filePath) throws URISyntaxException, IOException {
        Path path = Paths.get(ClassLoader.getSystemResource(filePath).toURI());
        StringBuilder sb = new StringBuilder();
        Files.lines(path).forEach(sb::append);
        return sb.toString();
    }

}
