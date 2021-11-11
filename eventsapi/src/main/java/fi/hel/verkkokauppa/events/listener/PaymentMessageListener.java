package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class PaymentMessageListener {

    private Logger log = LoggerFactory.getLogger(PaymentMessageListener.class);

    @Autowired
    private Environment env;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;


    @KafkaListener(
            topics = "payments",
            groupId="payments",
            containerFactory="paymentsKafkaListenerContainerFactory")
    private void paymentEventlistener(PaymentMessage message) {
        log.info("paymentEventlistener [{}]", message);

        if (EventType.PAYMENT_PAID.equals(message.getEventType())) {
            log.debug("event type is PAYMENT_PAID");
            paymentPaidAction(message);
            orderWebHookAction(message);
        }
        else if (EventType.PAYMENT_FAILED.equals(message.getEventType())) {
            log.debug("event type is PAYMENT_FAILED");
            paymentFailedAction(message);
        }
    }

    protected void orderWebHookAction(PaymentMessage message) {
        try {
            //read target url to call from env
            String url = env.getRequiredProperty("order.service.url");
            String path = "/order/payment-paid-webhook";
            log.debug("order.service.url is: " + url);
            log.debug("path is: " + path);

            //format payload, message to json string conversion
            PaymentMessage toCustomer = PaymentMessage.builder()
                    .eventType(message.getEventType())
                    .eventTimestamp(message.getEventTimestamp())
                    .paymentId(message.getPaymentId())
                    .orderId(message.getOrderId())
                    .namespace(message.getNamespace())
                    .paymentPaidTimestamp(message.getPaymentPaidTimestamp())
                    .build();
            String body = objectMapper.writeValueAsString(message);

            //send to target url
            restServiceClient.makePostCall(url + path, body);
        } catch (Exception e) {
            log.error("webhookAction: failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void paymentPaidAction(PaymentMessage message) {
        try {
            //read target url to call from env
            String url = env.getRequiredProperty("subscription.service.url");
            log.debug("subscription.service.url is: " + url);

            //format payload, message to json string conversion
            String body = objectMapper.writeValueAsString(message);

            //send to target url
            restServiceClient.makePostCall(url, body);
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void paymentFailedAction(PaymentMessage message) {
        // TODO action
        log.debug("TODO no action yet for PAYMENT_FAILED event");

    }


}