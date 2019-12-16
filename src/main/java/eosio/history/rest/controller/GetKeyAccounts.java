package eosio.history.rest.controller;


import eosio.history.rest.Actions;
import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.Key;
import eosio.history.rest.config.Properties;
import org.bitcoinj.core.Base58;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/history/get_key_accounts")
public class GetKeyAccounts {

    private ElasticSearchClient elasticSearchClient;
    private String get_key_accounts_index;

    @Autowired
    public void setProperties(Properties properties){
        this.get_key_accounts_index = properties.getNewAccountIndex();
    }
    @Autowired
    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient){
        this.elasticSearchClient = elasticSearchClient;
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    ResponseEntity<?> get_actions(@RequestBody Key key) throws IOException {
        Base58 base58 = new Base58();

        String testKey = base58.encode(key.getKey().getBytes());




        String response = new String();


        return new ResponseEntity<>(response.toString(), HttpStatus.OK);

    }

}
