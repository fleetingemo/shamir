package com.share.shamir.util.eosio.java.abieos.serialization;

import ch.qos.logback.classic.BasicConfigurator;
import io.jafka.jeos.EosApi;
import io.jafka.jeos.EosApiFactory;
import io.jafka.jeos.LocalApi;
import io.jafka.jeos.convert.Packer;
import io.jafka.jeos.core.common.SignArg;
import io.jafka.jeos.core.common.transaction.PackedTransaction;
import io.jafka.jeos.core.common.transaction.TransactionAction;
import io.jafka.jeos.core.common.transaction.TransactionAuthorization;
import io.jafka.jeos.core.request.chain.json2bin.TransferArg;
import io.jafka.jeos.core.request.chain.transaction.PushTransactionRequest;
import io.jafka.jeos.core.response.chain.ChainInfo;
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction;
import io.jafka.jeos.util.KeyUtil;
import io.jafka.jeos.util.Raw;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class jeos {
    private static String sign(String privateKey, SignArg arg, PackedTransaction t) {

        Raw raw = Packer.packPackedTransaction(arg.getChainId(), t);
        raw.pack(ByteBuffer.allocate(33).array());// TODO: what's this?
        String hash = KeyUtil.signHash(privateKey, raw.bytes());
        return hash;
    }
    public static void main(String[] args) throws Exception {
        EosApi client = EosApiFactory.create("http://10.28.217.174:10101", "http://10.28.217.174:10101", "http://10.28.217.174:10101");
        ChainInfo info = client.getChainInfo();
        System.out.println("chain info:"+info);
        callContract();
    }
    static void callContract() throws Exception{
        // --- get the current state of blockchain
        EosApi eosApi = EosApiFactory.create("http://10.28.217.174:10101");
        SignArg arg = eosApi.getSignArg(120);
        System.out.println(eosApi.getObjectMapper().writeValueAsString(arg));

        // --- sign the transation of token tansfer
        String privateKey = "5J61mY3dcgHb4egBYVWz4av68y24JzqteKRHMFrDXyhmQdbkhbr";//replace the real private key
        String from = "alice";
        LocalApi localApi = EosApiFactory.createLocalApi();
        //PushTransactionRequest req = localApi.transfer(arg, privateKey, from1, to1, quantity, memo);

        // ① pack transfer data
//        String name = "testname";
        TransferArg transferArg = new TransferArg(from, "bob", "25.0000 TXT", "something");
        String transferData = Packer.packTransfer(transferArg);
        //

        // ③ create the authorization
        List<TransactionAuthorization> authorizations = Arrays.asList(new TransactionAuthorization(from, "active"));

        // ④ build the all actions
        List<TransactionAction> actions = Arrays.asList(//
                new TransactionAction("eosio.token", "transfer", authorizations, transferData)//
        );

        // ⑤ build the packed transaction
        PackedTransaction packedTransaction = new PackedTransaction();
        packedTransaction.setExpiration(arg.getHeadBlockTime().plusSeconds(arg.getExpiredSecond()));
        packedTransaction.setRefBlockNum(arg.getLastIrreversibleBlockNum());
        packedTransaction.setRefBlockPrefix(arg.getRefBlockPrefix());

        packedTransaction.setMaxNetUsageWords(0);
        packedTransaction.setMaxCpuUsageMs(0);
        packedTransaction.setDelaySec(0);
        packedTransaction.setActions(actions);

        String hash = sign(privateKey, arg, packedTransaction);
        PushTransactionRequest req = new PushTransactionRequest();
        req.setTransaction(packedTransaction);
        req.setSignatures(Arrays.asList(hash));

        System.out.println(localApi.getObjectMapper().writeValueAsString(req));


        // --- push the signed-transaction to the blockchain
        PushedTransaction pts = eosApi.pushTransaction(req);
        System.out.println(localApi.getObjectMapper().writeValueAsString(pts));
    }
}