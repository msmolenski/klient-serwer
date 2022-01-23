package pl.amw.gdynia.kubernetes.ECB;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.amw.gdynia.kubernetes.dao.CurrencyRateRepository;
import pl.amw.gdynia.kubernetes.dao.entity.CurrencyRate;
import pl.amw.gdynia.kubernetes.schedule.CurrencyRateSchedule;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Configuration
class LoadDatabase {
    @Autowired
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);
    List<CurrencyRateFromECB> allCurrencies = new ArrayList<>();
    String[] availableCurrencies = {
            "usd", "jpy", "bgn", "czk", "dkk", "gbp", "huf", "pln", "ron", "sek", "chf", "isk", "nok", "hrk", "rub", "try",
            "aud", "brl", "cad", "cny", "hkd", "idr", "ils", "inr", "krw", "mxn", "myr", "nzd", "php", "sgd", "thb", "zar"
    };

    public void readData(String requestedCurrency) throws IOException, FeedException{
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
    }

    @Bean
    CommandLineRunner initDatabase(CurrencyRateRepository repository, CurrencyRateSchedule update) {
        return args -> {
            if (repository.findAll().isEmpty()){
                log.info("Database empty.");
                for(int i = 0; i < availableCurrencies.length; i++){
                    readData(availableCurrencies[i]);
                }
                BigDecimal exchangeRate;
                log.info("Adding values from ECB.");
                for (CurrencyRateFromECB i : allCurrencies) {
                    for (CurrencyRateFromECB j : allCurrencies) {
                        if (i.getCurrencyInitials().compareTo(j.getCurrencyInitials()) != 0 && i.getDate().compareTo(j.getDate()) == 0) {
                            // walutai -> walutaj
                            exchangeRate = j.getCurrencyValue().divide(i.getCurrencyValue(), 4, RoundingMode.HALF_UP);
                            repository.save(new CurrencyRate(i.getCurrencyInitials(), j.getCurrencyInitials(), exchangeRate, j.getDate()));
                        }
                    }
                    // € -> waluta
                    repository.save(new CurrencyRate("eur", i.getCurrencyInitials(), i.getCurrencyValue(), i.getDate()));
                    // waluta -> €
                    exchangeRate = new BigDecimal(1).divide(i.getCurrencyValue(), 4, RoundingMode.HALF_UP);
                    repository.save(new CurrencyRate(i.getCurrencyInitials(), "eur", exchangeRate, i.getDate()));
                }
                log.info("Values added.");
            } else {
                update.UpdateCurrencies();
            }
        };
    }
}