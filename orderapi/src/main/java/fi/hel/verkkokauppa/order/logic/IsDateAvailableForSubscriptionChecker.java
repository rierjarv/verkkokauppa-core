package fi.hel.verkkokauppa.order.logic;

import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class IsDateAvailableForSubscriptionChecker {

	private final IsDateWithinPausePeriodChecker isDateWithinPausePeriodChecker;

	@Autowired
	public IsDateAvailableForSubscriptionChecker(IsDateWithinPausePeriodChecker isDateWithinPausePeriodChecker) {
		this.isDateWithinPausePeriodChecker = isDateWithinPausePeriodChecker;
	}

	public boolean isDateAvailableForSubscription(
			Subscription subscription,
			LocalDate date
	) {
		return isDateAvailableForSubscription(subscription, date, null);
	}

	public boolean isDateAvailableForSubscription(
			Subscription subscription,
			LocalDate date,
			LocalDate dateAfter
	) {
		return isDateAvailableForSubscription(subscription, date, null, false);
	}

	public boolean isDateAvailableForSubscription(
			Subscription subscription,
			LocalDate date,
			LocalDate dateAfter,
			boolean ignoreExcludeDate
	) {
		return isDateAvailableForSubscription(subscription, date, null, false, false);
	}

	public boolean isDateAvailableForSubscription(
			Subscription subscription,
			LocalDate date,
			LocalDate dateAfter,
			boolean ignoreExcludeDate,
			boolean ignorePausePeriod
	) {

		return (ignorePausePeriod || !isDateWithinPausePeriodChecker.isDateWithinPausePeriod(subscription, date)) &&
				(ignoreExcludeDate/* || !Subscription.isExcludeDate(date)*/) /* TODO! */ &&
				(dateAfter == null || !date.isBefore(dateAfter)); // TODO: ok?
	}
}
