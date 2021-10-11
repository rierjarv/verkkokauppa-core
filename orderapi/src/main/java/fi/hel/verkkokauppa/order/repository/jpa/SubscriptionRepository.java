package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends BaseRepository<Subscription, String> {

    List<Subscription> findByCustomerId(String customerId);
}