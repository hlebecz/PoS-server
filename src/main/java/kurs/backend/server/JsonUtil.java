package kurs.backend.server;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.google.gson.*;

public final class JsonUtil {

  private JsonUtil() {}

  public static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(
              LocalDate.class,
              (JsonDeserializer<LocalDate>) (j, t, c) -> LocalDate.parse(j.getAsString()))
          .registerTypeAdapter(
              LocalDate.class,
              (JsonSerializer<LocalDate>) (d, t, c) -> new JsonPrimitive(d.toString()))
          .registerTypeAdapter(
              LocalTime.class,
              (JsonDeserializer<LocalTime>) (j, t, c) -> LocalTime.parse(j.getAsString()))
          .registerTypeAdapter(
              LocalTime.class,
              (JsonSerializer<LocalTime>) (t, ty, c) -> new JsonPrimitive(t.toString()))
          .registerTypeAdapter(
              LocalDateTime.class,
              (JsonDeserializer<LocalDateTime>) (j, t, c) -> LocalDateTime.parse(j.getAsString()))
          .registerTypeAdapter(
              LocalDateTime.class,
              (JsonSerializer<LocalDateTime>) (d, t, c) -> new JsonPrimitive(d.toString()))
          .create();
}
