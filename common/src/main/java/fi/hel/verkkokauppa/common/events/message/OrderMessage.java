package fi.hel.verkkokauppa.common.events.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage implements EventMessage {
    public String eventType;
    public String namespace;

    public String orderId;
    public String orderItemId;
    public String timestamp;

    public String orderType;

    public String productName;
    public String productQuantity;

    public String priceTotal;
    public String priceNet;
    public String vatPercentage;

    public String cardToken;

    public String isSubscriptionRenewalOrder;
    public String subscriptionId;
    public String userId;

    public OrderMessage toCustomerWebhook() {
        return OrderMessage
                .builder()
                .namespace(this.namespace)
                .orderId(this.orderId)
                .timestamp(this.timestamp)
                .eventType(this.eventType)
                .build();
    }

}
