package site.cpuis.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import site.cpuis.entity.ErrCode;

import java.io.IOException;

public class TypeSerializer extends JsonSerializer<ErrCode> {

    public void serialize(ErrCode value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeStartObject ();
        generator.writeFieldName ("errCode");
        generator.writeString ("0x" + Integer.toHexString (value.getCode ()));
        generator.writeFieldName ("errMsg");
        generator.writeString (value.getMsg ());
        generator.writeEndObject ();
    }
}
