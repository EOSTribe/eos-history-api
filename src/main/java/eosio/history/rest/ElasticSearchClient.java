package eosio.history.rest;

import eosio.history.rest.config.Properties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchClient {
    private RestHighLevelClient elasticsearchClient;
    private String ACTIONS_INDEX;
    private String TRANSACTION_INDEX;
    private String NEW_ACCOUNT_INDEX;
//    private String TRANSFER_INDEX;

    @Autowired
    public ElasticSearchClient(Properties properties){
        this.ACTIONS_INDEX = properties.getActionsIndex();
        this.TRANSACTION_INDEX = properties.getTransactionIndex();
        this.NEW_ACCOUNT_INDEX = properties.getNewAccountIndex();

        this.elasticsearchClient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(properties.getEsHost1(), 9200, "http"),
                        new HttpHost(properties.getEsHost2(), 9200, "http")));
    }

public RestHighLevelClient getElasticsearchClient(){
        return this.elasticsearchClient;
    }


}
