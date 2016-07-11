public class NoSuchFileException extends RuntimeException {
    public NoSuchFileException(String file) {
        super(file);
    }
}
