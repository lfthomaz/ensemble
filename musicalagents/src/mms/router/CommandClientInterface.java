package mms.router;

import mms.Command;

/**
 *
 * @author lfthomaz
 */
public interface CommandClientInterface {

    public String getAddress();

    public void input(CommandClientInterface cmdInterface, Command cmd);

}
