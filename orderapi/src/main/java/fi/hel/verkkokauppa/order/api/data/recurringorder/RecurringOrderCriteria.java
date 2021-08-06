package fi.hel.verkkokauppa.order.api.data.recurringorder;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class RecurringOrderCriteria implements Serializable {

	private static final long serialVersionUID = -8772317895908567093L;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // TODO: aika myös?
	private LocalDate activeAtDate;

	private String customerId;
	private String status;
	private String merchantNamespace;

	// TODO: 2 x address id?
}