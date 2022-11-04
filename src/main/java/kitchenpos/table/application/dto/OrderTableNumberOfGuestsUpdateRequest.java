package kitchenpos.table.application.dto;

public class OrderTableNumberOfGuestsUpdateRequest {

    private Integer numberOfGuests;

    public OrderTableNumberOfGuestsUpdateRequest() {
    }

    public OrderTableNumberOfGuestsUpdateRequest(Integer numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public Integer getNumberOfGuests() {
        return numberOfGuests;
    }
}
