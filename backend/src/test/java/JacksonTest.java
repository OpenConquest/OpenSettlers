import com.fasterxml.jackson.databind.ObjectMapper;
import fr.opensettlers.controller.GameMessage;

public class JacksonTest {
    public static void main(String[] args) throws Exception {
        String json = "{\"type\":\"LINK_FLAGS\",\"playerId\":0,\"flagIdA\":\"123e4567-e89b-12d3-a456-426614174000\",\"flagIdB\":\"123e4567-e89b-12d3-a456-426614174001\",\"path\":[{\"x\":1.0,\"y\":2.0}]}";
        ObjectMapper mapper = new ObjectMapper();
        GameMessage msg = mapper.readValue(json, GameMessage.class);
        System.out.println("Parsed: " + msg);
    }
}
