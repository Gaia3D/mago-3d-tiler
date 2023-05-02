package command;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] arg) {
        Command command = new Command();
        command.startCommand(arg);
    }
}
