package soc.common.server.data;

import java.util.List;

public interface UserProvider
{
    public User getUserByName(String name);

    public List<User> getAllUsers();

    public User getUserByID(int ID);

    public User registerUser(User user);
}
