package pl.amw.gdynia.kubernetes.schedule;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.amw.gdynia.kubernetes.dao.CurrencyRateRepository;
import pl.amw.gdynia.kubernetes.dao.entity.CurrencyRate;
import pl.amw.gdynia.kubernetes.ECB.CurrencyRateFromECB;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class CurrencyRateSchedule {
    @Autowired
    private CurrencyRateRepository repository;
    List<CurrencyRateFromECB> allCurrencies = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(CurrencyRateSchedule.class);
    boolean isConnected = true;
    String[] availableCurrencies = {
            "usd", "jpy", "bgn", "czk", "dkk", "gbp", "huf", "pln", "ron", "sek", "chf", "isk", "nok", "hrk", "rub", "try",
            "aud", "brl", "cad", "cny", "hkd", "idr", "ils", "inr", "krw", "mxn", "myr", "nzd", "php", "sgd", "thb", "zar"
    };

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public void readData(String requestedCurrency) throws FeedException {
        try {
            URL currenciesURL = new URL("https://www.ecb.europa.eu/rss/fxref-" + requestedCurrency + ".html");
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(currenciesURL));
            for(int i = 0; i < feed.getEntries().size(); i++){
                SyndEntry entry = feed.getEntries().get(i);
                double currencyValueDouble = Double.parseDouble(entry.getTitle().split(" ")[0]);
                BigDecimal currencyValue = BigDecimal.valueOf(currencyValueDouble);
                CurrencyRateFromECB row = new CurrencyRateFromECB(requestedCurrency, currencyValue, entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                allCurrencies.add(row);
            }
        } catch (IOException e) {
            isConnected = false;
        }
    }

    @Scheduled(cron = "00 30 17 * * MON-FRI", zone = "Europe/Warsaw")
    public void UpdateCurrencies() throws Exception {
        log.info("Updating...");
        for(int i = 0; i < availableCurrencies.length; i++){
            readData(availableCurrencies[i]);
        }
        BigDecimal exchangeRate;
        boolean updateNeeded = false;
        int count = 0;
        for (CurrencyRateFromECB i : allCurrencies) {
            for (CurrencyRateFromECB j : allCurrencies) {
                if (i.getCurrencyInitials().compareTo(j.getCurrencyInitials()) != 0 && i.getDate().compareTo(j.getDate()) == 0) {
                    exchangeRate = j.getCurrencyValue().divide(i.getCurrencyValue(), 4, RoundingMode.HALF_UP);
                    if (repository.findByCurrencyFromAndCurrencyToAndCurrencyRateAndDate(i.getCurrencyInitials(), j.getCurrencyInitials(), exchangeRate, j.getDate()).isEmpty()){
                        repository.save(new CurrencyRate(i.getCurrencyInitials(), j.getCurrencyInitials(), exchangeRate, j.getDate()));
                        updateNeeded = true;
                        count++;
                    }
                }
            }
            if (updateNeeded == true) {
                if (repository.findByCurrencyFromAndCurrencyToAndCurrencyRateAndDate("eur", i.getCurrencyInitials(), i.getCurrencyValue(), i.getDate()).isEmpty()){
                    // € -> waluta
                    repository.save(new CurrencyRate("eur", i.getCurrencyInitials(), i.getCurrencyValue(), i.getDate()));

                }
                if(repository.findByCurrencyFromAndCurrencyToAndCurrencyRateAndDate(i.getCurrencyInitials(), "eur", i.getCurrencyValue(), i.getDate()).isEmpty()){
                    // waluta -> €
                    exchangeRate = new BigDecimal(1).divide(i.getCurrencyValue(), 4, RoundingMode.HALF_UP);
                    repository.save(new CurrencyRate(i.getCurrencyInitials(), "eur", exchangeRate, i.getDate()));
                }
                updateNeeded = false;
            }
        }
        if (isConnected) {
            if(count > 0) {
                log.info("Updating finished.");
            } else {
                log.info("Nothing to update.");
            }
        } else {
            log.info("Connection not established after 5 retries.");
        }
    }
}
