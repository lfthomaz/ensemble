package mms.commands;

/**
 *
 * @author lfthomaz
 */
public interface CommandClientInterface {

    public String getAddress();

    public void input(CommandClientInterface cmdInterface, Command cmd);

}
