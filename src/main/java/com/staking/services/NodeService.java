package com.staking.services;

import com.staking.domain.Block;
import com.staking.domain.Transaction;
import com.staking.domain.TransactionOutput;
import com.staking.utils.FileUtils;
import com.staking.utils.RequestHelper;
import com.staking.resources.StakingResources;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.staking.resources.StakingResources.STORAGE_PREFIX;
import static com.staking.resources.StakingResources.REMOTE;

@Data
@Service
public class NodeService {

    private final String blockHeightEndpoint = "/nodes/height";

    private final long firstStepBlocks = 1000;

    @Value("${node.base}")
    private static String NODE_ENDPOINT = "";


    public long getBlockChainHeight(){
        return Long.parseLong(RequestHelper.GET_Request(NODE_ENDPOINT + blockHeightEndpoint));
    }


    public String getAddressHistory(String addr){
        return RequestHelper.GET_Request(NODE_ENDPOINT +"/getAddressHistory?addr="+addr).toString();
    }

    public JSONObject getBlock(long blockNumber){
        JSONObject jsonObject = null;
                //new JSONObject(RequestHelper.GET_Request(base + "/getBlock?blockHash=" + blockNumber));
        if (REMOTE) {
            jsonObject = new JSONObject(RequestHelper.GET_Request(NODE_ENDPOINT + "/getBlock?blockHash=" + blockNumber));
            return jsonObject;
        }

        Path path = FileUtils.getFirstFileByRegexp(STORAGE_PREFIX + "blocks/", blockNumber + "_*");
        if(path == null){

            System.err.println("Block with height:"+blockNumber);
            throw new RuntimeException();
        }
        String fileContent = FileUtils.readFromFilesystem(path);
        String filename = path.getFileName().toString();
        Block block = ObjectMapperService.stringToBlock(fileContent, filename.substring(filename.indexOf("_") + 1), blockNumber);

        JSONObject answer = new JSONObject();
        answer.put("hash", block.getHash());
        answer.put("height", block.getHeight());
        answer.put("previous", block.getPreviousHash());
        answer.put("timestamp", block.getTimestamp());
        answer.put("transactions", block.getTransactions());
        return answer;
    }

    public JSONArray getTransaction(String hash){
        if (REMOTE) return new JSONArray(RequestHelper.GET_Request(NODE_ENDPOINT + "/getTransaction?tx=" + hash));

        JSONArray answer = new JSONArray();

        try {
            Transaction transaction = ObjectMapperService.stringToTransaction(FileUtils.readFromFilesystem(STORAGE_PREFIX + "transactions/" + hash), hash);


            JSONObject part = new JSONObject();
            part.put("hash", transaction.getHash());
            part.put("from", transaction.getFrom() == null ? "" : transaction.getFrom());
            part.put("timestamp", transaction.getTimestamp());

            part.put("block", transaction.getHeight());


            JSONArray outputs = new JSONArray();
            for (TransactionOutput output : transaction.getOutputs()) {
                JSONObject object = new JSONObject();
                object.put("to", output.getRecipient());
                object.put("amount", output.getValue());
                outputs.put(object);
            }
            part.put("outputs", outputs);
            answer.put(part);


            return answer;
        }catch (Exception e){
            throw new RuntimeException("Can't map transaction: ["+e.getClass()+"] "+e.getMessage());
        }

    }

    public long getBalance(String address){
        return new JSONObject(RequestHelper.GET_Request(NODE_ENDPOINT + "/balance?address=" + address)).getLong("balance");
    }

    public String sendMoney(String from, String to, long amount){
        JSONObject payload = new JSONObject();
        payload.put("from", from);
        payload.put("to", to);
        payload.put("amount", amount);

        return RequestHelper.POST_Request(NODE_ENDPOINT +"/send", payload.toString(), "token", StakingResources.NODE_TOKEN);
    }


    @AllArgsConstructor
    @Data
    private static class TX {
        private String txHASH;
        private String from;
        private String to;
        private Long amount;
    }

    private static Map<String, Long> totalBalances = Collections.synchronizedMap(new HashMap<>());
}
