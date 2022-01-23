package pl.amw.gdynia.kubernetes.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.amw.gdynia.kubernetes.dao.entity.CurrencyRate;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {
    List<CurrencyRate> findTopByOrderByDateDesc();
    List<CurrencyRate> findByCurrencyFromAndCurrencyToAndCurrencyRateAndDate(String currencyFrom, String currencyTo, BigDecimal currencyRate, LocalDate date);
}