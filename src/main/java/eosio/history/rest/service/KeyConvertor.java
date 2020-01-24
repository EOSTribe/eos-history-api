package eosio.history.rest.service;

import one.block.eosiojava.error.utilities.EOSFormatterError;
import one.block.eosiojava.utilities.EOSFormatter;

import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class KeyConvertor {
    private EOSFormatter eosFormatter;

    public KeyConvertor(){
        eosFormatter = new EOSFormatter();
    }

    public String fromLegacyToK1PublicKey(String k) throws IOException, EOSFormatterError {
        return eosFormatter.convertPEMFormattedPublicKeyToEOSFormat(eosFormatter.convertEOSPublicKeyToPEMFormat(k),false );
    }
}
