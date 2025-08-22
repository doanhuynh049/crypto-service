package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.cryptoNotifier.model.MarketData;
import com.quat.cryptoNotifier.util.IndicatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataProviderService {

    @Autowired
    private MarketDataCacheService cacheService;

    public DataProviderService() {
    }

    /**
     * Public method that uses caching - this is what other services should call
     */
    public MarketData getMarketData(String coinGeckoId) {
        return cacheService.getMarketData(coinGeckoId);
    }
}
