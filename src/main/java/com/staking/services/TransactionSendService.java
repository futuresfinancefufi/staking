package com.staking.services;

import com.staking.resources.StakingResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Log4j2
@RequiredArgsConstructor
@Service
public class TransactionSendService {

    private final NodeService nodeService;
    private final int sleepTimeSeconds = 20;

    public void sendTransactions(Map<String, List<Long>> prizeAmounts) {
        for (String address : prizeAmounts.keySet()) {
            List<Long> winAmounts = prizeAmounts.get(address);
            winAmounts.forEach(wa->{
                try {

                    String response = nodeService.sendMoney(StakingResources.SEND_ADDRESS, address, wa);
                    log.info("Response from sending tokens to address " + address + " in amount " + wa + ":\n" + response);

                    while (response.contains("limitReached")){
                        log.info("Sleeping {} seconds before second try...", sleepTimeSeconds);

                        Thread.sleep(sleepTimeSeconds * 1000);
                        log.info("Second try of sending tokens response: {}", nodeService.sendMoney(StakingResources.SEND_ADDRESS, address, wa));
                    }

                    Thread.sleep(20*1000);
                } catch (Exception e) {
                    log.error("While sending to "+address + " in amount "+ wa + " got exception with message: "+e.getMessage());
                }
            });
        }
    }
}
