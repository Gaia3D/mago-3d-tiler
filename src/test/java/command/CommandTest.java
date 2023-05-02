package command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest {
    @Test
    @DisplayName("Test Command")
    void commandText() {
        String[] args = {"-i", "C:\\data\\sample\\Data3D\\Edumuseum_del_150417_02_3DS\\Edumuseum_del_150417_02.3ds", "-o", "C:\\data\\sample\\Data3D\\Edumuseum_del_150417_02_3DS\\Edumuseum_del_150417_02.gltf"};
        Command command = new Command();
        command.startCommand(args);
    }
}