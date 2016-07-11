import github.dmitmel.universal.tokenizer.Token;

public class ExpectedTokenNotFoundException extends RuntimeException {
    private Token token;
    public Token getToken() {
        return token;
    }

    public ExpectedTokenNotFoundException(Token token) {
        super(token.toString());
        this.token = token;
    }
}
