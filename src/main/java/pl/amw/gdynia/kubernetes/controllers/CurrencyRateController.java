package pl.amw.gdynia.kubernetes.controllers;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import pl.amw.gdynia.kubernetes.dao.CurrencyRateRepository;
import pl.amw.gdynia.kubernetes.dao.entity.CurrencyRate;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
public class CurrencyRateController {
    private final CurrencyRateRepository repository;
    CurrencyRateController(CurrencyRateRepository repository){
        this.repository = repository;
    }

    @GetMapping("/")
    public String index() {
        return "CurrencyRate app";
    }

    @GetMapping("/exchangeRate/")
    List<CurrencyRate> find(@RequestParam(value = "currencyFrom", defaultValue = "") String currencyFrom,
                            @RequestParam(value = "currencyTo", defaultValue = "") String currencyTo,
                            @RequestParam(value = "date", defaultValue = "") String date
    ) {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "date")).stream()
                .filter((e) -> {
                    if(!currencyFrom.equals("")){
                        return (e.getCurrencyFrom().equals(currencyFrom.toLowerCase(Locale.ROOT)));
                    } else return true;
                }).collect(Collectors.toList()).stream()
                .filter((e) -> {
                    if(!currencyTo.equals("")){
                        return (e.getCurrencyTo().equals(currencyTo.toLowerCase(Locale.ROOT)));
                    } else return true;
                })
                .collect(Collectors.toList()).stream()
                .filter((e) -> {
                    if (!date.equals("")) {
                        CurrencyRate mostRecentDate;
                        mostRecentDate = repository.findTopByOrderByDateDesc().get(0);
                        String recentDate = "";
                        recentDate = mostRecentDate.getDate().toString();
                        return (e.getDate().toString().equals(recentDate) || e.getDate().toString().equals(date));
                    } else return true;
                }).limit(date.equals("") ? repository.count() : 1).collect(Collectors.toList());
    }
}
