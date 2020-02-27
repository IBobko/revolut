package revolut.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joda.money.CurrencyUnit;

import java.lang.reflect.Type;

public class CurrencySerializer implements JsonSerializer<CurrencyUnit> {
    @Override
    public JsonElement serialize(CurrencyUnit currencyUnit, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(currencyUnit.getCode());
    }
}
