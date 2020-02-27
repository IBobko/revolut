package revolut.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joda.money.Money;
import org.joda.money.format.MoneyFormatter;
import org.joda.money.format.MoneyFormatterBuilder;

import java.lang.reflect.Type;

public class MoneySerializer implements JsonSerializer<Money> {
    @Override
    public JsonElement serialize(Money money, Type type, JsonSerializationContext jsonSerializationContext) {
        final MoneyFormatter formatter = new MoneyFormatterBuilder()
                .appendCurrencySymbolLocalized()
                .appendLiteral(" ")
                .appendAmountLocalized()
                .toFormatter();
        return new JsonPrimitive(formatter.print(money));
    }
}