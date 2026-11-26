package br.edu.ifpr.hackflow.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Data e hora atuais no formato ISO-8601 sem fuso (ex.: 2026-10-23T09:12:00), horário de Brasília. */
public final class Tempo {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Tempo() {
    }

    public static String agora() {
        return LocalDateTime.now(FUSO)
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /** Data e hora daqui a N horas, no mesmo formato (usado no vencimento da sessão). */
    public static String daquiAHoras(long horas) {
        return LocalDateTime.now(FUSO)
                .plusHours(horas)
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
