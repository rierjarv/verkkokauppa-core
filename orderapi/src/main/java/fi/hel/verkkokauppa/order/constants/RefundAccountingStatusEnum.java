package fi.hel.verkkokauppa.order.constants;

public enum RefundAccountingStatusEnum {
    CREATED("created"),
    EXPORTED("exported");

    private final String status;

    RefundAccountingStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }
}