package pl.amw.gdynia.kubernetes.dao.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
public class CurrencyRate {
    private @Id @GeneratedValue Long id;
    private String currencyFrom;
    private String currencyTo;
    @Column(precision=16, scale=4)
    private BigDecimal currencyRate;
    private LocalDate date;

    public CurrencyRate() {}
    public CurrencyRate(String currencyFrom, String currencyTo, BigDecimal currencyRate, LocalDate date) {
        this.currencyFrom = currencyFrom;
        this.currencyTo = currencyTo;
        this.currencyRate = currencyRate;
        this.date = date;
    }

}
