package fi.hel.verkkokauppa.order.model.renewal;

import java.time.LocalDateTime;

import fi.hel.verkkokauppa.shared.model.Identifiable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "subscription_renewal_requests")
public class SubscriptionRenewalRequest implements Identifiable {
    @Id
    private String id;
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime renewalRequested;

}
