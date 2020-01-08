package eosio.history.rest;

import java.util.Objects;

public class TransferDeduplicator {
    private String block_timestamp;
    private String block_num;
    private String quantity;

    public TransferDeduplicator(String block_timestamp, String block_num, String quantity) {
        this.block_timestamp = block_timestamp;
        this.block_num = block_num;
        this.quantity = quantity;
    }

    public String getBlock_timestamp() {
        return block_timestamp;
    }

    public void setBlock_timestamp(String block_timestamp) {
        this.block_timestamp = block_timestamp;
    }

    public String getBlock_num() {
        return block_num;
    }

    public void setBlock_num(String block_num) {
        this.block_num = block_num;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferDeduplicator transferDeduplicator = (TransferDeduplicator) o;
        return Objects.equals(block_timestamp, transferDeduplicator.block_timestamp) &&
                Objects.equals(block_num, transferDeduplicator.block_num) &&
                Objects.equals(quantity, transferDeduplicator.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block_timestamp, block_num, quantity);
    }
}

