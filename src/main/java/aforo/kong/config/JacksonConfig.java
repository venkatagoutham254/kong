package aforo.kong.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATETIME_FORMAT = "dd MMM, yyyy HH:mm z"; // e.g. 06 Jan, 2025 08:58 IST
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_FORMAT).withZone(ZoneId.of("Asia/Kolkata"));

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .featuresToDisable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                )
                // ✅ Global serializer for LocalDateTime in IST
                .serializers(new LocalDateTimeSerializer(FORMATTER));
    }
}
