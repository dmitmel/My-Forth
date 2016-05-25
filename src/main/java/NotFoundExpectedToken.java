import org.universal.tokenizer.Token;

public class NotFoundExpectedToken extends RuntimeException {
    private Token token;
    public Token getToken() {
        return token;
    }

    public NotFoundExpectedToken(Token token) {
        super(token.toString());
        this.token = token;
    }
}
