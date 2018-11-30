package org.gsc.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.crypto.cryptohash.Keccak256;
import org.gsc.crypto.cryptohash.Keccak512;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.SmartContract;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import static java.util.Arrays.copyOfRange;

/**
 * @Auther: kay
 * @Date: 11/30/18 16:04
 * @Description:
 */
@Slf4j
public class RPCUtils {

    private static final Provider CRYPTO_PROVIDER;

    private static final String HASH_256_ALGORITHM_NAME;
    private static final String HASH_512_ALGORITHM_NAME;



    static {
        Provider provider;
        Provider p = Security.getProvider("SC");

        provider = (p != null) ? p : new BouncyCastleProvider();
        provider.put("MessageDigest.GSC-KECCAK-256", Keccak256.class.getName());
        provider.put("MessageDigest.GSC-KECCAK-512", Keccak512.class.getName());

        Security.addProvider(provider);
        CRYPTO_PROVIDER = Security.getProvider("SC");
        HASH_256_ALGORITHM_NAME = "GSC-KECCAK-256";
        HASH_512_ALGORITHM_NAME = "GSC-KECCAK-512";
    }

    public static SmartContract.ABI jsonStr2ABI(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
        JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
        SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
        for (int index = 0; index < jsonRoot.size(); index++) {
            JsonElement abiItem = jsonRoot.get(index);
            boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null &&
                    abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
            boolean constant = abiItem.getAsJsonObject().get("constant") != null &&
                    abiItem.getAsJsonObject().get("constant").getAsBoolean();
            String name = abiItem.getAsJsonObject().get("name") != null ?
                    abiItem.getAsJsonObject().get("name").getAsString() : null;
            JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null ?
                    abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
            JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null ?
                    abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
            String type = abiItem.getAsJsonObject().get("type") != null ?
                    abiItem.getAsJsonObject().get("type").getAsString() : null;
            boolean payable = abiItem.getAsJsonObject().get("payable") != null &&
                    abiItem.getAsJsonObject().get("payable").getAsBoolean();
            String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null ?
                    abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
            if (type == null) {
                logger.error("No type!");
                return null;
            }
            if (!type.equalsIgnoreCase("fallback") && null == inputs) {
                logger.error("No inputs!");
                return null;
            }

            SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
            entryBuilder.setAnonymous(anonymous);
            entryBuilder.setConstant(constant);
            if (name != null) {
                entryBuilder.setName(name);
            }

            /* { inputs : optional } since fallback function not requires inputs*/
            if (null != inputs) {
                for (int j = 0; j < inputs.size(); j++) {
                    JsonElement inputItem = inputs.get(j);
                    if (inputItem.getAsJsonObject().get("name") == null ||
                            inputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Input argument invalid due to no name or no type!");
                        return null;
                    }
                    String inputName = inputItem.getAsJsonObject().get("name").getAsString();
                    String inputType = inputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    paramBuilder.setIndexed(false);
                    paramBuilder.setName(inputName);
                    paramBuilder.setType(inputType);
                    entryBuilder.addInputs(paramBuilder.build());
                }
            }

            /* { outputs : optional } */
            if (outputs != null) {
                for (int k = 0; k < outputs.size(); k++) {
                    JsonElement outputItem = outputs.get(k);
                    if (outputItem.getAsJsonObject().get("name") == null ||
                            outputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Output argument invalid due to no name or no type!");
                        return null;
                    }
                    String outputName = outputItem.getAsJsonObject().get("name").getAsString();
                    String outputType = outputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    paramBuilder.setIndexed(false);
                    paramBuilder.setName(outputName);
                    paramBuilder.setType(outputType);
                    entryBuilder.addOutputs(paramBuilder.build());
                }
            }

            entryBuilder.setType(getEntryType(type));
            entryBuilder.setPayable(payable);
            if (stateMutability != null) {
                entryBuilder.setStateMutability(getStateMutability(stateMutability));
            }

            abiBuilder.addEntrys(entryBuilder.build());
        }

        return abiBuilder.build();
    }

    private static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
        switch (type) {
            case "constructor":
                return SmartContract.ABI.Entry.EntryType.Constructor;
            case "function":
                return SmartContract.ABI.Entry.EntryType.Function;
            case "event":
                return SmartContract.ABI.Entry.EntryType.Event;
            case "fallback":
                return SmartContract.ABI.Entry.EntryType.Fallback;
            default:
                return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
        }
    }

    private static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
            String stateMutability) {
        switch (stateMutability) {
            case "pure":
                return SmartContract.ABI.Entry.StateMutabilityType.Pure;
            case "view":
                return SmartContract.ABI.Entry.StateMutabilityType.View;
            case "nonpayable":
                return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
            case "payable":
                return SmartContract.ABI.Entry.StateMutabilityType.Payable;
            default:
                return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
        }
    }

    public static String printTransaction(Protocol.Transaction transaction){
       // Protocol.Transaction transaction = transactionExtention.getTransaction();
        JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction));
        JSONArray contracts = new JSONArray();
        transaction.getRawData().getContractList().stream().forEach(contract -> {
            try {
                JSONObject contractJson = null;
                Any contractParameter = contract.getParameter();
                switch (contract.getType()) {
                    case AccountCreateContract:
                        Contract.AccountCreateContract accountCreateContract = contractParameter
                                .unpack(Contract.AccountCreateContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(accountCreateContract));
                        break;
                    case TransferContract:
                        Contract.TransferContract transferContract = contractParameter.unpack(Contract.TransferContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(transferContract));
                        break;
                    case TransferAssetContract:
                        Contract.TransferAssetContract transferAssetContract = contractParameter
                                .unpack(Contract.TransferAssetContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(transferAssetContract));
                        break;
                    case VoteAssetContract:
                        Contract.VoteAssetContract voteAssetContract = contractParameter.unpack(Contract.VoteAssetContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(voteAssetContract));
                        break;
                    case VoteWitnessContract:
                        Contract.VoteWitnessContract voteWitnessContract = contractParameter
                                .unpack(Contract.VoteWitnessContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(voteWitnessContract));
                        break;
                    case WitnessCreateContract:
                        Contract.WitnessCreateContract witnessCreateContract = contractParameter
                                .unpack(Contract.WitnessCreateContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessCreateContract));
                        break;
                    case AssetIssueContract:
                        Contract.AssetIssueContract assetIssueContract = contractParameter
                                .unpack(Contract.AssetIssueContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(assetIssueContract));
                        break;
                    case WitnessUpdateContract:
                        Contract.WitnessUpdateContract witnessUpdateContract = contractParameter
                                .unpack(Contract.WitnessUpdateContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessUpdateContract));
                        break;
                    case ParticipateAssetIssueContract:
                        Contract.ParticipateAssetIssueContract participateAssetIssueContract = contractParameter
                                .unpack(Contract.ParticipateAssetIssueContract.class);
                        contractJson = JSONObject
                                .parseObject(JsonFormat.printToString(participateAssetIssueContract));
                        break;
                    case AccountUpdateContract:
                        Contract.AccountUpdateContract accountUpdateContract = contractParameter
                                .unpack(Contract.AccountUpdateContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(accountUpdateContract));
                        break;
                    case FreezeBalanceContract:
                        Contract.FreezeBalanceContract freezeBalanceContract = contractParameter
                                .unpack(Contract.FreezeBalanceContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(freezeBalanceContract));
                        break;
                    case UnfreezeBalanceContract:
                        Contract.UnfreezeBalanceContract unfreezeBalanceContract = contractParameter
                                .unpack(Contract.UnfreezeBalanceContract.class);
                        contractJson = JSONObject
                                .parseObject(JsonFormat.printToString(unfreezeBalanceContract));
                        break;
                    case UnfreezeAssetContract:
                        Contract.UnfreezeAssetContract unfreezeAssetContract = contractParameter
                                .unpack(Contract.UnfreezeAssetContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(unfreezeAssetContract));
                        break;
                    case WithdrawBalanceContract:
                        Contract.WithdrawBalanceContract withdrawBalanceContract = contractParameter
                                .unpack(Contract.WithdrawBalanceContract.class);
                        contractJson = JSONObject
                                .parseObject(JsonFormat.printToString(withdrawBalanceContract));
                        break;
                    case UpdateAssetContract:
                        Contract.UpdateAssetContract updateAssetContract = contractParameter
                                .unpack(Contract.UpdateAssetContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(updateAssetContract));
                        break;
                    case CreateSmartContract:
                        Contract.CreateSmartContract deployContract = contractParameter
                                .unpack(Contract.CreateSmartContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract));
                        byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
                        byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
                        jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
                        break;
                    case TriggerSmartContract:
                        Contract.TriggerSmartContract triggerSmartContract = contractParameter
                                .unpack(Contract.TriggerSmartContract.class);
                        contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract));
                        break;
                    // todo add other contract
                    default:
                }
                JSONObject parameter = new JSONObject();
                parameter.put("value", contractJson);
                parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
                JSONObject jsonContract = new JSONObject();
                jsonContract.put("parameter", parameter);
                jsonContract.put("type", contract.getType());
                contracts.add(jsonContract);
            } catch (InvalidProtocolBufferException e) {
                logger.debug("InvalidProtocolBufferException: {}", e.getMessage());
            }
        });

        JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
        rawData.put("contract", contracts);
        jsonTransaction.put("raw_data", rawData);
        String txID = ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
        jsonTransaction.put("txID", txID);
        return jsonTransaction.toJSONString();
    }

    public static byte[] generateContractAddress(Protocol.Transaction trx, byte[] ownerAddress) {
        String HASH_256_ALGORITHM_NAME = "GSC-KECCAK-256";
        String HASH_512_ALGORITHM_NAME = "GSC-KECCAK-512";
        // get tx hash
        byte[] txRawDataHash = Sha256Hash.of(trx.getRawData().toByteArray()).getBytes();

        // combine
        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
                    CRYPTO_PROVIDER);
            digest.update(combined);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }

        byte[] hash = digest.digest();
        byte[] address = copyOfRange(hash, 11, hash.length);
        address[0] = Constant.ADD_PRE_FIX_BYTE_MAINNET;
        return address;
    }
}
