package et.yoseph.spatialcon.conversion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("convert")
public class ConvertProperties {
    /**
     * Folder location for storing files
     */
    @Value("${spatialcon.storage.location}")

    private String location;

    /**
     * Path to the command
     */
    @Value("${spatialcon.command.location}")
    private String command;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
