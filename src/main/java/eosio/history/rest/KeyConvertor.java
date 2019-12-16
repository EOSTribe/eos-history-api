package eosio.history.rest;

import org.bitcoinj.core.Base58;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;

public class KeyConvertor {
    private static final int publicKeyDataSize = 33;


    public void stringToPublicKey(String k) throws IOException {
        Base58 base58 = new Base58();
        byte[] whole = base58.decode(k.substring(3));
        byte[] out = new byte[publicKeyDataSize];


        byte[] key = new byte[publicKeyDataSize];

        for (int i=0; i < publicKeyDataSize;i++){
            key[i] = whole[i];
        }

        byte[] kb = k.getBytes("UTF-8");
        RIPEMD160Digest d = new RIPEMD160Digest();
        d.update (kb, 0, kb.length);
        byte[] o = new byte[d.getDigestSize()];
        d.doFinal (o, 0);
        Hex.encode (o, out);

    }

/**
    export function stringToPublicKey(s: string): Key {
        if (typeof s !== 'string') {
            throw new Error('expected string containing public key');
        }
        if (s.substr(0, 3) === 'EOS') {
        const whole = base58ToBinary(publicKeyDataSize + 4, s.substr(3));
        const key = { type: KeyType.k1, data: new Uint8Array(publicKeyDataSize) };
            for (let i = 0; i < publicKeyDataSize; ++i) {
                key.data[i] = whole[i];
            }
        const digest = new Uint8Array(ripemd160(key.data));
            if (digest[0] !== whole[publicKeyDataSize] || digest[1] !== whole[34]
                    || digest[2] !== whole[35] || digest[3] !== whole[36]) {
                throw new Error('checksum doesn\'t match');
            }
            return key;
        } else if (s.substr(0, 7) === 'PUB_K1_') {
            return stringToKey(s.substr(7), KeyType.k1, publicKeyDataSize, 'K1');
        } else if (s.substr(0, 7) === 'PUB_R1_') {
            return stringToKey(s.substr(7), KeyType.r1, publicKeyDataSize, 'R1');
        } else {
            throw new Error('unrecognized public key format');
        }
    }

    /** Convert `key` to string (base-58) form
    export function publicKeyToString(key: Key) {
        if (key.type === KeyType.k1 && key.data.length === publicKeyDataSize) {
            return keyToString(key, 'K1', 'PUB_K1_');
        } else if (key.type === KeyType.r1 && key.data.length === publicKeyDataSize) {
            return keyToString(key, 'R1', 'PUB_R1_');
        } else {
            throw new Error('unrecognized public key format');
        }
    }

    /** If a key is in the legacy format (`EOS` prefix), then convert it to the new format (`PUB_K1_`).
     * Leaves other formats untouched

    export function convertLegacyPublicKey(s: string) {
        if (s.substr(0, 3) === 'EOS') {
            return publicKeyToString(stringToPublicKey(s));
        }
        return s;
    }

    */
}
