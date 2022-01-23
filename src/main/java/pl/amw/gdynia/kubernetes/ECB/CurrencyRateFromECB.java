package pl.amw.gdynia.kubernetes.ECB;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CurrencyRateFromECB {
    private String currencyInitials;
    private BigDecimal currencyValue;
    private LocalDate date;

    public CurrencyRateFromECB(String currencyInitials, BigDecimal currencyValue, LocalDate date) {
        this.currencyInitials = currencyInitials;
        this.currencyValue = currencyValue;
        this.date = date;
    }
}
