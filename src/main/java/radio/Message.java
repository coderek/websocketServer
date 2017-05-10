package radio;

/**
 * Created by coderek on 09/05/17.
 */
public class Message {
    public String getStationName() {
        return stationName;
    }

    private String stationName;

    public Message(String stationName) {
        this.stationName = stationName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private String title;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    private String body;


    public String toString() {
        return String.format("%s\n%s\n", getTitle(), getBody());
    }
}
