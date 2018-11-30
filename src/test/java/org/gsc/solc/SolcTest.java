package org.gsc.solc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.gsc.api.GrpcAPI;
import org.gsc.api.WalletGrpc;
import org.gsc.client.WalletGrpcClient;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.solidity.compiler.CompilationResult;
import org.gsc.solidity.compiler.SolidityCompiler;
import org.gsc.utils.JsonFormat;
import org.gsc.utils.RPCUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.Optional;

import static org.gsc.solidity.compiler.SolidityCompiler.Options.*;

public class SolcTest {


    @Test
    public void solcTest() throws IOException {
        String contractSrc =
               "pragma solidity ^0.4.25;\n" +
                        "contract cont {" +
                        "        function name() returns (string) {return \"cont\";}\n" +
                        "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");

        System.out.println("-------------------------------------------------------------------------------------------");

        CompilationResult result = CompilationResult.parse(res.output);
        System.out.println("result: " + result.getContractName());

        CompilationResult.ContractMetadata contractMetadata = result.getContract("cont");
        System.out.println("ABI:          " + contractMetadata.abi);
        System.out.println("Bytecode:     " + contractMetadata.bin);
        System.out.println("metadata:     " + contractMetadata.metadata);
        System.out.println("getInterface: " + contractMetadata.getInterface());

        System.out.println("-------------------------------------------------------------------------------------------");

    }

    @Test
    public void getVotes() {
        String host = "127.0.0.1";
        int port = 50051;
        WalletGrpcClient walletGrpcClient = new WalletGrpcClient(host, port);

        Optional<GrpcAPI.NodeList> nodelist = walletGrpcClient.listNodes();

        System.out.println("-------------------------------------------------------------------------------------------");
        System.out.println(JsonFormat.printToString(nodelist.get()));
        System.out.println("-------------------------------------------------------------------------------------------");
    }

    @Test
    public void deployContract() throws IOException {
        String originAddress = "262daebb11f20b68a2035519a8553b597bb7dbbfa4";
        String node = "47.254.71.98:50051";
        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub walletBlockingStub = null;
        channel = ManagedChannelBuilder.forTarget(node).usePlaintext(true).build();
        walletBlockingStub = WalletGrpc.newBlockingStub(channel);

        String contractName = "cont";
        String contractSrc =
                "pragma solidity ^0.4.25;\n" +
                        "contract cont {" +
                        "        function name() returns (string) {return \"cont\";}\n" +
                        "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");

        System.out.println("---------------------------------------------------------------------------------");

        CompilationResult result = null;
        try {
            result = CompilationResult.parse(res.output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("result: " + result.getContractName());

        CompilationResult.ContractMetadata contractMetadata = result.getContract(result.getContractName());

        String abiStr = contractMetadata.abi;
        String byteCode = contractMetadata.bin;
        long callValue = 0;
        long consumeUserResourcePercent = 0;

        Protocol.SmartContract.ABI abi = RPCUtils.jsonStr2ABI(abiStr);

        Protocol.SmartContract.Builder smartContract = Protocol.SmartContract.newBuilder();
        smartContract.setOriginAddress(ByteString.copyFrom(Hex.decode(originAddress)));
        // smartContract.setContractAddress();
        smartContract.setName(result.getContractName());
        smartContract.setAbi(abi);
        smartContract.setCallValue(callValue);
        smartContract.setBytecode(ByteString.copyFrom(Hex.decode(byteCode)));
        smartContract.setConsumeUserResourcePercent(consumeUserResourcePercent);
        Contract.CreateSmartContract request = Contract.CreateSmartContract.newBuilder()
                .setNewContract(smartContract).setOwnerAddress(ByteString.copyFrom(Hex.decode(originAddress))).build();
        GrpcAPI.TransactionExtention response = walletBlockingStub.deployContract(request);
        if (response.getTransaction() != null){
            System.out.println("Contract: \n" + RPCUtils.printTransaction(response.getTransaction()));
            System.out.println(response.getResult().getResult());
        }
    }
}

