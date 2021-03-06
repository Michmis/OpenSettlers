package soc.common.server;

import soc.common.actions.lobby.LobbyAction;
import soc.common.server.data.User;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("lobby")
public interface ServerService extends RemoteService
{
    /**
     * Login and setup a CometSession on the chat server.
     * 
     * @param username
     * @throws ChatException
     */
    public User login(String nickName, String password);

    /**
     * Logout and destroy the CometSession on the chat server.
     * 
     * @param username
     * @throws ChatException
     */
    public void logout(User user);

    /**
     * Send a message to all users on the chat server.
     * 
     * @param message
     * @throws ChatException
     */
    public void send(LobbyAction action);

}
