package com.realtime.myfriend.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.HttpURLConnection;
import java.net.URL;


@Component
@Slf4j
public class HearBeat {

    @Value("${health.check}")
    private  String healthCheckUrl;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void heartBeat() {
        try {
            URL url = new URL(healthCheckUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.getResponseCode();
            conn.disconnect();
            log.info("heart beat running.....");
        } catch (Exception e) {
            log.error("heart beat failing .....",e);
        }
    }
}
