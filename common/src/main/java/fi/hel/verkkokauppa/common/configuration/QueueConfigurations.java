package fi.hel.verkkokauppa.common.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Data
public class QueueConfigurations {

    @Value("${queue.order.notifications:order-notifications}")
    String orderNotificationsQueue;

    @Value("${queue.subscription.notifications:subscription-notifications}")
    String subscriptionNotificationsQueue;

    @Value("${queue.subscription.renewals:subscription-renewals}")
    String subscriptionRenewalsQueue;

    public List<String> getAll() {
        ArrayList<String> allQueus = new ArrayList<>();
        allQueus.add(orderNotificationsQueue);
        allQueus.add(subscriptionNotificationsQueue);
        allQueus.add(subscriptionRenewalsQueue);
        return allQueus;
    }
}
