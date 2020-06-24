package com.staking.services;

import com.staking.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.CronExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.staking.resources.StakingResources.*;

@Service
@Log4j2
@AllArgsConstructor
@Data
public class StakingService {

    private final Object LOCK = new Object();
    private final List<String> allWallets = FileUtils.readListString(ALL_WALLETS_FILE_NAME);

    private final List<String> notParticipants = List.of(SEND_ADDRESS, "D3F927065EC33CCAC35832D25AD4A54AFD646BD0", "728B4E9D5E113A09AB12DC9876C08E1F8FF927AB", "DD132B7FF6B139610B72DD968FCFD30EAB5FF7E7", "96D3A2300F7ED2930D0F55AE5E01E4887555CFE2");
    private final List<String> masterNodeParticipants = new ArrayList<>();


    private final NodeService nodeService;
    private final TransactionSendService txSender;

    @Scheduled(cron = "0 47 */1 * * *")
    public void processStaking() throws ParseException, InterruptedException {
        log.info("Started staking");
        synchronized (LOCK){
            if (allWallets.isEmpty()) throw new RuntimeException("Wallets is empty!");

            CronExpression ce = new CronExpression(PERFORM_PATTERN);
            if (isStakingTime(ce)){
                long lastLotteryBlock = getLastStakingBlock();
                log.info("Prev lottery: "+new Date(getMonthAgo()));
//                LotteryInfo lotteryInfo = collectParticipantInfo(lastLotteryBlock==0?prevDate:nodeService.getBlock(lastLotteryBlock).getLong("timestamp")); // for starting by
                StakingInfo stakingInfo = collectParticipantInfo(nodeService.getBlock(lastLotteryBlock).getLong("timestamp"));

                Map<String, List<Long>> prizeAmounts = calculatePrizeAmounts(stakingInfo);
                log.info("Total participants: {}\nPrizeAmounts: {}", prizeAmounts.keySet().size(), prizeAmounts);
                Thread.sleep(60*1000);
                txSender.sendTransactions(prizeAmounts);
                log.info("Finished staking.");
            }
        }
    }


    private Map<String, List<Long>> calculatePrizeAmounts(StakingInfo stakingInfo) {
        double masterNodeBonus = stakingInfo.prize*0.05;
        log.info("Total master node bonus: "+masterNodeBonus);
        var mnInfo = new Object(){private long totalBalance = 0;};

        log.info("Total master node balances: "+mnInfo.totalBalance);
        stakingInfo.participants.forEach((k, v)->{
            if (masterNodeParticipants.contains(k)) mnInfo.totalBalance+=v;
        });

        Map<String, List<Long>> prizeAmount = new HashMap<>();
        stakingInfo.prize*=0.9;
        log.info("Total prize: "+ stakingInfo.prize);

        double totalBalances = stakingInfo.participants.values().stream().mapToLong(Long::longValue).sum();
        log.info("Total wallets balances: "+totalBalances);
        for (String address : stakingInfo.participants.keySet()) {
            List<Long> winAmounts = getWinAmountForAddress(stakingInfo, totalBalances, address);
            if (masterNodeParticipants.contains(address)) {
                double multiplier = Double.valueOf(stakingInfo.participants.get(address)) / masterNodeBonus;
                final double mnWinAmount = masterNodeBonus * multiplier;
                long win = (long) mnWinAmount;
                if (win>0) winAmounts.add(win);
            }

            prizeAmount.put(address, winAmounts);
        }

        return prizeAmount;
    }

    private List<Long> getWinAmountForAddress(StakingInfo stakingInfo, double totalBalances, String address) {
        final double multiplier = Double.valueOf(stakingInfo.participants.get(address)) / totalBalances;
        final double winAmount = stakingInfo.prize * multiplier;


        List<Long> winAmounts = new ArrayList<>();
        long win = (long) winAmount;
        if (win>0) winAmounts.add(win);
        return winAmounts;
    }

    @SneakyThrows
    private StakingInfo collectParticipantInfo(long prevDate) {
        StakingInfo stakingInfo = new StakingInfo();

        long currentHeight = nodeService.getBlockChainHeight();
        long finishBlock = 0;

        log.info("Starting from block "+currentHeight);
        FileUtils.rewriteToFile(STAKING_LAST_BLOCK_FILE_NAME, String.valueOf(currentHeight));

        while (true){
            JSONObject block = nodeService.getBlock(currentHeight);
            if (block.getLong("timestamp") <= prevDate) {
                finishBlock = block.getLong("height");
                break;
            }
            if (currentHeight%10==0) System.out.println("Current block "+currentHeight);
            JSONArray transactions = block.getJSONArray("transactions");
            checkBlockTransactions(stakingInfo, transactions);
            currentHeight--;
        }

        log.info("Finished collecting from block: "+currentHeight + "\nTo block: "+finishBlock);
        return stakingInfo;
    }

    private void checkBlockTransactions(StakingInfo stakingInfo, JSONArray transactions) {
        for (int i = 0; i < transactions.length(); i++) {
            String hash = transactions.getString(i);
            JSONArray transaction = nodeService.getTransaction(hash);
            String from = transaction.getJSONObject(0).getString("from");
            if (from.equals(SEND_ADDRESS)) continue;
            if (from.isEmpty()){
                stakingInfo.prize+=transaction.getJSONObject(0).getJSONArray("outputs").getJSONObject(0).getLong("amount");
                continue;
            }
            JSONArray outputs = transaction.getJSONObject(0).getJSONArray("outputs");
            for (int output = 0; output < outputs.length(); output++) {
                try{
                    String toAddress = outputs.getJSONObject(output).getString("to");
                    checkAddress(stakingInfo, from);
                    checkAddress(stakingInfo, toAddress);
                }catch (Exception ignored){};
            }
            System.out.println(transaction);
        }
    }

    private void checkAddress(StakingInfo stakingInfo, String addr) {
        if (!stakingInfo.participants.containsKey(addr) && !stakingInfo.addressesSmallBalance.contains(addr) && allWallets.contains(addr) && !notParticipants.contains(addr)){
            long balance = nodeService.getBalance(addr);
            if (balance<MIN_BALANCE){
                log.info("Address {} added to small balances list with balance: {}", addr, balance);
                stakingInfo.addressesSmallBalance.add(addr);
            }
            else{
                log.info("Address {} added to participants list with balance {}", addr, balance);
                stakingInfo.participants.put(addr, balance);
            }
        }
    }

    private boolean isStakingTime(CronExpression ce) throws InterruptedException {

        Date nextValidTimeAfter = ce.getNextValidTimeAfter(new Date());
        log.info("Next time to staking "+nextValidTimeAfter);
        long timeToStaking = nextValidTimeAfter.getTime() - new Date().getTime();
        if (timeToStaking/1000 < 25 * 60 * 1000){

            while (nextValidTimeAfter.getTime()-new Date().getTime() > 0){
                log.info("Starting staking soon. "+(nextValidTimeAfter.getTime()-new Date().getTime())/1000/60 + " minutes remaining...");
                Thread.sleep(5000);
            }
            return true;
        }
        return false;
    }

    //currrently uging by last block
    private long defineTimeStartStaking(CronExpression ce) throws ParseException {

        log.info("Defining start staking");
        Date nextValidTimeAfter = ce.getNextValidTimeAfter(new Date());

        Date nextnext = ce.getNextValidTimeAfter(nextValidTimeAfter);
        log.info("Next staking "+nextValidTimeAfter + " and next "+nextnext);

        long prevLotteryMilliss = nextnext.getTime() - nextValidTimeAfter.getTime();
        return new Date().getTime()-prevLotteryMilliss;
    }

    private long getLastStakingBlock(){
        try {

            return Long.parseLong(FileUtils.readFromFilesystem(STAKING_LAST_BLOCK_FILE_NAME).replaceAll("\n", ""));
        } catch (Exception e) {
            System.err.println("While getting last staking block: "+e.getMessage());
            return 0;
        }
    }

    private static long getMonthAgo(){
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return monthAgo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Data
    private static class StakingInfo {
        private long prize;

        private Set<String> addressesSmallBalance = new HashSet<>();
        private Map<String, Long> participants = new HashMap<>();
    }

}
