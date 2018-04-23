package playlagom.sharelocation.models;

/**
 * Created by User on 4/12/2018.
 */

public class User {
    private String name;
    private String email;
    private String password;
    private String danger;

    public User() {}
    public User(String email, String password) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public String getDanger() {
        return danger;
    }
    public void setDanger(String danger) {
        this.danger = danger;
    }
}
