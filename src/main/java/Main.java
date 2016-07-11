import github.dmitmel.jargparse.ArgumentParseException;
import github.dmitmel.jargparse.ArgumentParser;
import github.dmitmel.jargparse.ParsingResult;
import github.dmitmel.jargparse.Flag;
import github.dmitmel.jargparse.Positional;
import github.dmitmel.jargparse.util.IterableUtils;
import github.dmitmel.universal.tokenizer.*;

import java.io.*;
import java.util.*;

public class Main {
    public static final String APP_NAME = "myforth";
    public static final double TRUE = -1.0;
    public static final double FALSE = 0.0;

    private Stack<Object> stack = new Stack<>();
    private Map<String, List<Token>> words = new HashMap<>(0);
    private boolean stopped = false;
    private Tokenizer tokenizer;
    private StringBuilder allProgram = new StringBuilder(0);
    private boolean higherDebugMode = false;
    private Object variable;
    private List<String> initialStackValues;

    private static void delay(long millis) {
        try {
            Thread.sleep(millis);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws IOException {
        new Main().main0(args);
    }

    private void main0(String[] args) throws IOException {
        ArgumentParser argumentParser = new ArgumentParser(APP_NAME, "My Forth language interpreter.");

        argumentParser.addArgument(new Positional("file with program (if not specified - using interactive mode)",
                "FILE",
                Positional.Usage.OPTIONAL,
                "<stdin>"));
        argumentParser.addArgument(new Flag("-hdm",
                "--higher-debug-mode",
                "use higher debug mode. For example - print stack traces of underlying Java exceptions",
                "HIGHER_DEBUG_MODE"));
        argumentParser.addArgument(new Positional("initial stack values",
                "INITIAL_STACK_VALUES",
                Positional.Usage.ZERO_OR_MORE));

        try {
            ParsingResult parsedArguments = argumentParser.run(args);

            if (parsedArguments.getBoolean("SHOW_HELP")) {
                System.out.println(argumentParser.constructHelpMessage());
                System.exit(0);
            } else if (parsedArguments.getBoolean("SHOW_VERSION")) {
                System.out.println("1.0");
                System.exit(0);
            } else {
                tokenizer = new Tokenizer();
                tokenizer.singleLineCommentSequence = "\\";
                tokenizer.multilineCommentStart = "(*";
                tokenizer.multilineCommentEnd = "*)";

                higherDebugMode = parsedArguments.getBoolean("HIGHER_DEBUG_MODE");
                String inputFileString = parsedArguments.getString("FILE");
                initialStackValues = parsedArguments.getList("INITIAL_STACK_VALUES");
                addInitialStackValuesToStack();

                if (parsedArguments.get("FILE").equals("<stdin>")) {
                    startInInteractiveMode();
                } else {
                    startInFileInterpretationMode(inputFileString);
                }
            }
        } catch (ArgumentParseException e) {
            System.out.println(argumentParser.parsingExceptionToString(e));
        }
    }

    private void addInitialStackValuesToStack() {
        List<Token> initStackValuesTokens = tokenizer.toTokenList(IterableUtils.join(initialStackValues, " "));
        for (Token token : initStackValuesTokens) {
            switch (token.getType()) {
                case NUMBER:
                    stack.add(token.getValue());
                    break;

                case STRING:
                    stack.add(((StringToken) token).getValueWithoutQuotes());
                    break;
            }
        }
    }

    private void startInFileInterpretationMode(String inputFileString) throws IOException {
        File inputFileObj = new File(inputFileString);
        if (!inputFileObj.exists()) {
            inputFileObj = new File(System.getProperty("user.dir") + File.separatorChar + inputFileString);
            if (!inputFileObj.exists()) {
                System.err.printf("%s: %s: No such file or directory\n", APP_NAME, inputFileString);
                System.exit(1);
            }
        }
        char[] buffer = new char[(int) inputFileObj.length()];
        FileReader fileReader = new FileReader(inputFileObj);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        bufferedReader.read(buffer);
        bufferedReader.close();
        fileReader.close();
        allProgram.append(buffer);

        List<Token> tokens = tokenizer.toTokenList(allProgram.toString());
        interpretTokensWithErrorHandling(tokens);
    }

    private void startInInteractiveMode() throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Welcome to My-Forth-Interpreter 1.0 in interactive mode!");
        System.out.printf("%s, Java %s\n", System.getProperty("java.vm.name"),
                System.getProperty("java.runtime.version"));
        System.out.printf("on %s %s %s\n", System.getProperty("os.name"), System.getProperty("os.arch"),
                System.getProperty("os.version"));

        while (!stopped) {
            System.out.print("\n>>> ");

            String line = input.readLine();
            allProgram.append(line).append('\n');

            List<Token> tokens = line != null ? tokenizer.toTokenList(line) :
                    Collections.singletonList(new LiteralToken("bye"));
            interpretTokensWithErrorHandling(tokens);
        }
    }

    private void interpretTokensWithErrorHandling(List<Token> tokens) {
        try {
            interpretTokensWithoutErrorHandling(tokens);
        } catch (Exception e) {
            System.out.println();
            if (higherDebugMode)
                e.printStackTrace();
            delay(5);
        }
    }

    private void interpretTokensWithoutErrorHandling(List<Token> tokens) {
        for (ListIterator<Token> iterator = tokens.listIterator(); iterator.hasNext(); ) {
            Token token = iterator.next();

            switch (token.getType()) {
                case NUMBER:
                    stack.add(token.getValue());
                    break;

                case STRING:
                    stack.add(((StringToken) token).getValueWithoutQuotes());
                    break;

                case LITERAL:
                    String realString = ((String) token.getValue()).toLowerCase();
                    if (realString.equals("bye")) {
                        stopped = true;

                    } else if (realString.equals("dup")) {
                        Object lastItem = stack.pop();
                        stack.push(lastItem);
                        stack.push(lastItem);

                    } else if (realString.equals("invert")) {
                        double lastItem = (double) stack.pop();
                        stack.push(0 - lastItem + 1);

                    } else if (realString.equals("max")) {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(Math.max(op1, op2));

                    } else if (realString.equals("min")) {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(Math.min(op1, op2));

                    } else if (realString.equals("if")) {
                        double lastItem = (double) stack.pop();

                        try {
                            ListIterator<Token> iteratorCopy = tokens.listIterator(iterator.nextIndex());
                            List<Token> ifBody = findTokensBefore(new LiteralToken("else"), iteratorCopy);
                            List<Token> elseBody = findTokensBefore(new LiteralToken("then"), iteratorCopy);

                            if (lastItem == TRUE)
                                interpretTokensWithoutErrorHandling(ifBody);
                            else if (lastItem == FALSE)
                                interpretTokensWithoutErrorHandling(elseBody);
                            else
                                interpretTokensWithoutErrorHandling(elseBody);
                        } catch (NoSuchElementException e) {
                            List<Token> ifBody = findTokensBefore(new LiteralToken("then"), iterator);
                            if (lastItem == TRUE)
                                interpretTokensWithoutErrorHandling(ifBody);
                        }

                    } else if (realString.equals("stack")) {
                        System.out.printf("<%d> ", stack.size());
                        System.out.printf("%s ", IterableUtils.join(stack, " "));

                    } else if (realString.equals("quine")) {
                        System.out.printf("%s ", allProgram);

                    } else if (realString.equals("cr")) {
                        System.out.println();

                    } else if (realString.equals("do")) {
                        List<Token> loopBody = findTokensBefore(new LiteralToken("loop"), iterator);
                        int lower = ((Double) stack.pop()).intValue();
                        int upper = ((Double) stack.pop()).intValue();

                        for (int i = lower; i <= upper; i++) {
                            interpretTokensWithoutErrorHandling(loopBody);
                        }

                    } else if (realString.equals("undef")) {
                        String word = (String) stack.pop();
                        words.remove(word);

                    } else if (realString.equals("swap")) {
                        Object op1 = stack.pop();
                        Object op2 = stack.pop();
                        stack.push(op1);
                        stack.push(op2);

                    } else if (realString.equals("drop")) {
                        stack.pop();

                    } else if (realString.equals("store_var")) {
                        variable = stack.pop();

                    } else if (realString.equals("get_var")) {
                        Objects.requireNonNull(variable);
                        stack.push(variable);

                    } else {
                        List<Token> wordBody = words.get(((String) token.getValue()).toLowerCase());
                        interpretTokensWithoutErrorHandling(wordBody);

                    }

                    break;

                case SINGLE_CHAR:
                    char realVal = (char) token.getValue();
                    if (realVal == '+') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 + op2);

                    } else if (realVal == '-') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 - op2);

                    } else if (realVal == '/') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 / op2);

                    } else if (realVal == '*') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 * op2);

                    } else if (realVal == '.') {
                        System.out.print(stack.pop());
                        System.out.print(" ");

                    } else if (realVal == ':') {
                        String wordName = ((LiteralToken) iterator.next()).getValue().toLowerCase();
                        List<Token> wordBody = findTokensBefore(new SingleCharToken(';'), iterator);

                        if (words.containsKey(wordName))
                            System.out.printf(" redefined word \"%s\" ", wordName);
                        words.put(wordName, wordBody);

                    } else if (realVal == '=') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 == op2 ? TRUE : FALSE);

                    } else if (realVal == '>') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 > op2 ? TRUE : FALSE);

                    } else if (realVal == '<') {
                        double op2 = (double) stack.pop();
                        double op1 = (double) stack.pop();
                        stack.push(op1 < op2 ? TRUE : FALSE);

                    }

                    break;
            }
        }
    }

    private List<Token> findTokensBefore(Token needed, Iterator<Token> iterator) {
        List<Token> tokensBefore = new ArrayList<>(0);

        boolean receivedNeeded = false;
        while (!receivedNeeded) {
            Token wordBodyPart = iterator.next();
            if (wordBodyPart.getType() == needed.getType() &&
                    wordBodyPart.getValue().equals(needed.getValue()))
                receivedNeeded = true;
            else
                tokensBefore.add(wordBodyPart);
        }

        return tokensBefore;
    }
}
