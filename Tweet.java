package v1;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/*
v1.Tweet objects contain the message being sent between processes as well the UTC timestamp
 */

public class Tweet {
    public int id;
    public String msg;
    public long time;

    public Tweet(){}

    public Tweet(int id, String msg){
        this.id = id;
        this.msg = msg;
        this.time = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
    }

    public Tweet(Tweet x) {
        this.id = x.id;
        this.msg = x.msg;
        this.time = x.time;
    }
}
