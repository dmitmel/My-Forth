public class UnknownBooleanValueException extends RuntimeException {
    private double number;
    public double getNumber() {
        return number;
    }

    public UnknownBooleanValueException(double number) {
        super(Double.toString(number));
        this.number = number;
    }
}
