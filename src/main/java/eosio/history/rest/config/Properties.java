package eosio.history.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@PropertySource("classpath:global.properties")
@ConfigurationProperties
public class Properties {
    @Value("${ES_HOST1}")
    private String esHost1;
    @Value("${ES_HOST2}")
    private String esHost2;
    @Value("${ACTIONS_INDEX}")
    private String actionsIndex;
    @Value("${TRANSACTION_INDEX}")
    private String transactionIndex;
    @Value("${TRANSFER_ACTIONS_INDEX}")
    private String transferActionsIndex;
    @Value("${ACCOUNT_ACTIONS_INDEX}")
    private String accountActionsIndex;
    @Value("${AccessControlAllowOrigin}")
    private String accessControlAllowOrigin;
    @Value("${AccessControlAllowHeaders}")
    private String[] accessControlAllowHeaders;

    public String getAccessControlAllowOrigin() {
        return accessControlAllowOrigin;
    }

    public void setAccessControlAllowOrigin(String accessControlAllowOrigin) {
        this.accessControlAllowOrigin = accessControlAllowOrigin;
    }

    public String[] getAccessControlAllowHeaders() {
        return accessControlAllowHeaders;
    }

    public void setAccessControlAllowHeaders(String[] accessControlAllowHeaders) {
        this.accessControlAllowHeaders = accessControlAllowHeaders;
    }

    public String getEsHost1() {
        return esHost1;
    }

    public void setEsHost1(String esHost1) {
        this.esHost1 = esHost1;
    }

    public String getEsHost2() {
        return esHost2;
    }

    public void setEsHost2(String esHost2) {
        this.esHost2 = esHost2;
    }

    public String getActionsIndex() {
        return actionsIndex;
    }

    public void setActionsIndex(String actionsIndex) {
        this.actionsIndex = actionsIndex;
    }

    public String getTransactionIndex() {
        return transactionIndex;
    }

    public void setTransactionIndex(String transactionIndex) {
        this.transactionIndex = transactionIndex;
    }

    public String getTransferActionsIndex() {
        return transferActionsIndex;
    }

    public void setTransferActionsIndex(String transferActionsIndex) {
        this.transferActionsIndex = transferActionsIndex;
    }

    public String getAccountActionsIndex() {
        return accountActionsIndex;
    }

    public void setAccountActionsIndex(String accountActionsIndex) {
        this.accountActionsIndex = accountActionsIndex;
    }
}
