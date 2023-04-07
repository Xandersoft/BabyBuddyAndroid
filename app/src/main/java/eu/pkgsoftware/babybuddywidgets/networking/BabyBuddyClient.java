package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import eu.pkgsoftware.babybuddywidgets.Constants;
import eu.pkgsoftware.babybuddywidgets.CredStore;

public class BabyBuddyClient extends StreamReader {
    public final boolean DEBUG = false;

    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssX";
    public static final String DATE_QUERY_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

    public static class ACTIVITIES {
        public static final String SLEEP = "sleep";
        public static final String TUMMY_TIME = "tummy-times";
        public static final String FEEDING = "feedings";

        public static final String[] ALL = new String[3];

        static {
            ALL[0] = FEEDING;
            ALL[1] = SLEEP;
            ALL[2] = TUMMY_TIME;
        }

        public static int index(String s) {
            for (int i = 0; i < ALL.length; i++) {
                if (Objects.equals(ALL[i], s)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static class EVENTS {
        public static final String CHANGE = "changes";

        public static final String[] ALL = new String[1];

        static {
            ALL[0] = CHANGE;
        }

        public static int index(String s) {
            for (int i = 0; i < ALL.length; i++) {
                if (Objects.equals(ALL[i], s)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static Date parseNullOrDate(JSONObject o, String field) throws JSONException, ParseException {
        if (o.isNull(field)) {
            return null;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);

        // Remove milliseconds
        String strDate = o.getString(field);
        strDate = strDate.replaceAll("\\.[0-9]+([+-Z])", "$1");
        strDate = strDate.replaceAll("Z$", "+00:00");
        return sdf.parse(strDate);
    }

    private static String dateToString(Date date) {
        if (date == null) {
            return null;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);
        return sdf.format(date);
    }

    private static String dateToQueryString(Date date) {
        if (date == null) {
            return null;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_QUERY_FORMAT_STRING);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public static class QueryValues {
        public HashMap<String, String> queryValues = new HashMap<String, String>();

        public QueryValues add(String name, String value) {
            queryValues.put(name, value);
            return this;
        }

        public QueryValues add(String name, int value) {
            return this.add(name, "" + value);
        }

        public QueryValues add(String name, Date value) {
            return this.add(name, dateToQueryString(value));
        }

        public String toQueryString() {
            StringBuilder result = new StringBuilder("");
            for (Map.Entry<String, String> e : queryValues.entrySet()) {
                if (result.length() > 0) {
                    result.append("&");
                }
                result.append(e.getKey());
                result.append("=");
                result.append(urlencode(e.getValue()));
            }
            return result.toString();
        }

        public JSONObject toJsonObject() {
            return new JSONObject(queryValues);
        }
    }

    public static class Child {
        public int id;
        public String slug;
        public String first_name;
        public String last_name;
        public String birth_date;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Child child = (Child) o;
            return id == child.id &&
                Objects.equals(slug, child.slug) &&
                Objects.equals(first_name, child.first_name) &&
                Objects.equals(last_name, child.last_name) &&
                Objects.equals(birth_date, child.birth_date);
        }

        public static Child fromJSON(String s) throws JSONException {
            return fromJSON(new JSONObject(s));
        }

        public static Child fromJSON(JSONObject obj) throws JSONException {
            Child c = new Child();
            c.id = obj.getInt("id");
            c.slug = obj.getString("slug");
            c.first_name = obj.getString("first_name");
            c.last_name = obj.getString("last_name");
            c.birth_date = obj.getString("birth_date");
            return c;
        }

        public JSONObject toJSON() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("slug", slug);
                o.put("first_name", first_name);
                o.put("last_name", last_name);
                o.put("birth_date", birth_date);
            } catch (JSONException e) {
                throw new RuntimeException("ERROR should not happen");
            }
            return o;
        }
    }

    public static class Timer {
        public int id;
        public Integer child_id;
        public String name;
        public Date start;
        public Date end;
        public boolean active;
        public int user_id;

        public String readableName() {
            if (name != null) {
                return name;
            }
            return "Quick timer #" + id;
        }

        public Date computeCurrentServerEndTime(BabyBuddyClient client) {
            if (end != null) {
                return end;
            }
            long serverMillis = new Date().getTime() + client.getServerDateOffsetMillis();
            return new Date(serverMillis);
        }

        public static Timer fromJSON(JSONObject obj) throws JSONException, ParseException {
            Timer t = new Timer();
            t.id = obj.getInt("id");
            t.child_id = obj.isNull("child") ? null : obj.getInt("child");
            t.name = obj.isNull("name") ? null : obj.getString("name");
            t.start = parseNullOrDate(obj, "start");
            t.end = parseNullOrDate(obj, "end");
            t.active = obj.getBoolean("active");
            t.user_id = obj.getInt("user");
            return t;
        }

        public JSONObject toJSON() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("child", child_id);
                o.put("name", name);
                o.put("start", dateToString(start));
                o.put("end", dateToString(end));
                o.put("active", active);
                o.put("user", user_id);
            } catch (JSONException e) {
                throw new RuntimeException("ERROR should not happen");
            }
            return o;
        }

        @Override
        public String toString() {
            return "Timer{" +
                "id=" + id +
                ", child_id=" + child_id +
                ", name='" + name + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", active=" + active +
                ", user_id=" + user_id +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Timer timer = (Timer) o;
            return id == timer.id && active == timer.active && user_id == timer.user_id && Objects.equals(child_id, timer.child_id) && Objects.equals(name, timer.name) && Objects.equals(start, timer.start) && Objects.equals(end, timer.end);
        }

        public Timer clone() {
            Timer result = new Timer();
            result.id = id;
            result.child_id = child_id;
            result.name = name;
            result.start = start;
            result.end = end;
            result.active = active;
            result.user_id = user_id;
            return result;
        }
    }

    public static class TimeEntry {
        public String type;
        public int typeId;
        public Date start;
        public Date end;
        public String notes;

        public static TimeEntry fromJsonObject(JSONObject json, String type) throws JSONException, ParseException {
            String notes = null;
            if (json.has("milestone")) {
                notes = json.getString("milestone");
            }
            if (json.has("notes")) {
                notes = json.getString("notes");
            }
            return new TimeEntry(
                type,
                json.getInt("id"),
                parseNullOrDate(json, "start"),
                parseNullOrDate(json, "end"),
                notes == null ? "" : notes
            );
        }

        public TimeEntry(String type, int typeId, Date start, Date end, String notes) {
            this.type = type;
            this.typeId = typeId;
            this.start = start;
            this.end = end;
            this.notes = notes;
        }

        @Override
        public String toString() {
            return "TimeEntry{" +
                "type='" + type + '\'' +
                ", typeId=" + typeId +
                ", start=" + start +
                ", end=" + end +
                ", notes='" + notes + '\'' +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeEntry timeEntry = (TimeEntry) o;
            return typeId == timeEntry.typeId &&
                Objects.equals(type, timeEntry.type) &&
                Objects.equals(start, timeEntry.start) &&
                Objects.equals(end, timeEntry.end) &&
                Objects.equals(notes, timeEntry.notes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, typeId, start, end, notes);
        }

        public String getUserPath() {
            switch (this.type) {
                case ACTIVITIES.FEEDING:
                    return "/feedings/" + this.typeId + "/";
                case EVENTS.CHANGE:
                    return "/changes/" + this.typeId + "/";
                case ACTIVITIES.TUMMY_TIME:
                    return "/tummy-time/" + this.typeId + "/";
                case ACTIVITIES.SLEEP:
                    return "/sleep/" + this.typeId + "/";
                default:
                    System.err.println("WARNING! getUserPath not implemented for type: " + this.type);
            }
            return null;
        }

        public String getApiPath() {
            return "/api/" + this.type + "/" + this.typeId + "/";
        }
    }

    public static class ChangeEntry extends TimeEntry {
        public boolean wet;
        public boolean solid;

        public ChangeEntry(String type, int typeId, Date start, Date end, String notes, boolean wet, boolean solid) {
            super(type, typeId, start, end, notes);
            this.wet = wet;
            this.solid = solid;
        }

        @Override
        public String toString() {
            return "ChangeEntry{" +
                "type='" + type + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", notes='" + notes + '\'' +
                ", wet=" + wet +
                ", solid=" + solid +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ChangeEntry that = (ChangeEntry) o;
            return wet == that.wet && solid == that.solid;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), wet, solid);
        }
    }

    public static class FeedingEntry extends TimeEntry {
        public Constants.FeedingMethodEnum feedingMethod;
        public Constants.FeedingTypeEnum feedingType;

        public FeedingEntry(
            String type,
            int typeId,
            Date start,
            Date end,
            String notes,
            Constants.FeedingMethodEnum feedingMethod,
            Constants.FeedingTypeEnum feedingType) {
            super(type, typeId, start, end, notes);
            this.feedingMethod = feedingMethod;
            this.feedingType = feedingType;
        }

        @Override
        public String toString() {
            return "FeedingEntry{" +
                "type='" + type + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", notes='" + notes + '\'' +
                ", feedingMethod=" + feedingMethod +
                ", feedingType=" + feedingType +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            FeedingEntry that = (FeedingEntry) o;
            return feedingMethod == that.feedingMethod && feedingType == that.feedingType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), feedingMethod, feedingType);
        }
    }

    public static class RequestCodeFailure extends IOException {
        public String response;

        public RequestCodeFailure(String response) {
            this.response = response;
        }
    }

    public interface RequestCallback<R> {
        void error(@NotNull Exception error);

        void response(R response);
    }

    private final SimpleDateFormat SERVER_DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    private Handler syncMessage;
    private CredStore credStore;
    private Looper mainLoop;
    private long serverDateOffset = -1000;

    private void updateServerDateTime(HttpURLConnection con) {
        String dateString = con.getHeaderField("Date");
        try {
            Date serverTime = SERVER_DATE_FORMAT.parse(dateString);
            Date now = new Date(System.currentTimeMillis());

            serverDateOffset = serverTime.getTime() - now.getTime() - 100; // 100 ms offset
        } catch (ParseException e) {
        }
    }

    public URL pathToUrl(String path) throws MalformedURLException {
        String prefix = credStore.getServerUrl().replaceAll("/*$", "");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new URL(prefix + "/" + path);
    }

    private HttpURLConnection doQuery(String path) throws IOException {
        HttpURLConnection con = (HttpURLConnection) pathToUrl(path).openConnection();
        String token = credStore.getAppToken();
        con.setRequestProperty("Authorization", "Token " + token);
        con.setDoInput(true);
        return con;
    }

    @NonNull
    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date now = new Date(System.currentTimeMillis() + serverDateOffset);
        return sdf.format(now);
    }

    public BabyBuddyClient(Looper mainLoop, CredStore credStore) {
        this.mainLoop = mainLoop;
        this.credStore = credStore;
        this.syncMessage = new Handler(mainLoop);
    }

    private void dispatchQuery(String method, String path, String payload, RequestCallback<String> callback) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection query = doQuery(path);
                    if (DEBUG) {
                        System.out.println(
                            "BabyBuddyClient.DEBUG "
                                + method
                                + "  path: " + path
                                + "  payload: " + payload
                        );
                    }

                    query.setRequestMethod(method);
                    if (payload != null) {
                        query.setDoOutput(true);
                        query.setRequestProperty("Content-Type", "application/json; utf-8");
                        query.setRequestProperty("Accept", "application/json");
                        OutputStream os = query.getOutputStream();
                        os.write(payload.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    query.connect();
                    updateServerDateTime(query);

                    int responseCode = query.getResponseCode();
                    if (DEBUG) {
                        System.out.println(" -> response code: " + responseCode);
                    }
                    if ((responseCode < 200) || (responseCode >= 300)) {
                        String message = query.getResponseMessage();
                        throw new RequestCodeFailure(message);
                    }


                    String result = loadHttpData(query);
                    syncMessage.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.response(result);
                        }
                    });
                } catch (Exception e) {
                    syncMessage.post(() -> callback.error(e));
                }
            }
        };
        thread.start();
    }

    public void listChildren(RequestCallback<Child[]> callback) {
        dispatchQuery("GET", "api/children/", null, new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray children = obj.getJSONArray("results");
                    List<Child> result = new ArrayList<Child>(children.length());
                    for (int i = 0; i < children.length(); i++) {
                        JSONObject c = children.getJSONObject(i);
                        result.add(Child.fromJSON(c));
                    }
                    callback.response(result.toArray(new Child[0]));
                } catch (JSONException e) {
                    this.error(e);
                }
            }
        });
    }

    public void listTimers(RequestCallback<Timer[]> callback) {
        listTimers(null, callback);
    }

    public void listTimers(Integer child_id, RequestCallback<Timer[]> callback) {
        String queryString = "?" + (child_id == null ? "" : ("child=" + child_id));
        dispatchQuery("GET", "api/timers/" + queryString, null, new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray timers = obj.getJSONArray("results");
                    List<Timer> result = new ArrayList<>(timers.length());
                    for (int i = 0; i < timers.length(); i++) {
                        JSONObject item = timers.getJSONObject(i);
                        result.add(Timer.fromJSON(item));
                    }
                    result.sort(Comparator.comparingInt(t -> t.id));
                    callback.response(result.toArray(new Timer[0]));
                } catch (JSONException | ParseException e) {
                    this.error(e);
                }
            }
        });
    }

    public void deleteTimer(int timer_id, RequestCallback<Boolean> callback) {
        dispatchQuery("DELETE", String.format("api/timers/%d/", timer_id), null, new RequestCallback<String>() {
            @Override
            public void error(Exception error) {
                callback.error(error);
            }

            @Override
            public void response(String response) {
                callback.response(true);
            }
        });
    }

    private static String urlencode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public void setTimerActive(int timer_id, boolean active, RequestCallback<Boolean> callback) {
        dispatchQuery(
            "PATCH",
            "api/timers/" + timer_id + (active ? "/restart/" : "/stop/"),
            null,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public long getServerDateOffsetMillis() {
        return serverDateOffset;
    }

    public void getTimer(int timer_id, RequestCallback<Timer> callback) {
        dispatchQuery(
            "GET",
            "api/timers/" + timer_id + "/",
            null,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    JSONObject obj = null;
                    try {
                        obj = new JSONObject(response);
                        callback.response(Timer.fromJSON(obj));
                    } catch (JSONException | ParseException e) {
                        error(e);
                    }
                }
            });
    }

    public void createSleepRecordFromTimer(Timer timer, String notes, RequestCallback<Boolean> callback) {
        String data;
        try {
            data = (new JSONObject())
                .put("timer", timer.id)
                .put("notes", notes)
                .toString();
        } catch (JSONException e) {
            throw new RuntimeException("JSON Structure not built correctly");
        }

        dispatchQuery(
            "POST",
            "api/sleep/",
            data,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void createTummyTimeRecordFromTimer(Timer timer, String milestone, RequestCallback<Boolean> callback) {
        String data;
        try {
            data = (new JSONObject())
                .put("timer", timer.id)
                .put("milestone", milestone)
                .toString();
        } catch (JSONException e) {
            throw new RuntimeException("JSON Structure not built correctly");
        }

        dispatchQuery(
            "POST",
            "api/tummy-times/",
            data,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void createFeedingRecordFromTimer(Timer timer, String type, String method, Float amount, String notes, RequestCallback<Boolean> callback) {
        String data;
        try {
            data = (new JSONObject())
                .put("timer", timer.id)
                .put("type", type)
                .put("method", method)
                .put("amount", amount)
                .put("notes", notes)
                .toString();
        } catch (JSONException e) {
            throw new RuntimeException("JSON Structure not built correctly");
        }

        dispatchQuery(
            "POST",
            "api/feedings/",
            data,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void createChangeRecord(Child child, boolean wet, boolean solid, String notes, RequestCallback<Boolean> callback) {
        String data;
        try {
            data = (new JSONObject())
                .put("child", child.id)
                .put("time", now())
                .put("wet", wet)
                .put("solid", solid)
                .put("color", "")
                .put("amount", null)
                .put("notes", notes)
                .toString();
        } catch (JSONException e) {
            throw new RuntimeException("JSON Structure not built correctly");
        }

        dispatchQuery(
            "POST",
            "api/changes/",
            data,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void createTimer(Child child, String name, RequestCallback<Timer> callback) {
        String data;
        try {
            data = (new JSONObject())
                .put("child", child.id)
                .put("name", name)
                .put("start", now())
                .toString();
        } catch (JSONException e) {
            throw new RuntimeException("JSON Structure not built correctly");
        }

        dispatchQuery(
            "POST",
            "api/timers/",
            data,
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    Timer result;
                    try {
                        result = Timer.fromJSON(new JSONObject(response));
                    } catch (JSONException | ParseException e) {
                        error(e);
                        return;
                    }
                    callback.response(result);
                }
            }
        );
    }

    public void listGeneric(String activity, QueryValues queryValues, RequestCallback<JSONArray> callback) {
        String path = "api/" + activity + "/";
        if (queryValues != null) {
            path = path + "?" + queryValues.toQueryString();
        }
        dispatchQuery("GET", path, null, new RequestCallback<String>() {
            @Override
            public void error(@NonNull Exception error) {
                callback.error(error);
            }

            @Override
            public void response(String response) {
                JSONArray result = null;
                try {
                    JSONObject listResponse = new JSONObject(response);
                    result = listResponse.getJSONArray("results");
                } catch (JSONException e) {
                    error(e);
                    return;
                }
                if (result == null) {
                    callback.response(new JSONArray());
                } else {
                    callback.response(result);
                }
            }
        });
    }

    private interface WrapTimelineEntry<TE extends TimeEntry> {
        TE wrap(JSONObject json) throws ParseException, JSONException;
    }

    private class GenericTimelineRequest<TE extends TimeEntry> {
        private final Class<TE> runtimeClass;

        public GenericTimelineRequest(Class<TE> cls) {
            runtimeClass = cls;
        }

        private TE[] emptyArray() {
            return (TE[]) Array.newInstance(runtimeClass, 0);
        }

        private void genericTimelineRequest(
            String target,
            int child_id,
            int offset,
            int count,
            RequestCallback<TE[]> callback,
            WrapTimelineEntry<TE> wrapper) {

            listGeneric(
                target,
                new QueryValues()
                    .add("child", child_id)
                    .add("offset", offset)
                    .add("limit", count),
                new RequestCallback<JSONArray>() {
                    @Override
                    public void error(@NotNull Exception error) {
                        callback.error(error);
                    }

                    @Override
                    public void response(JSONArray objects) {
                        List<TE> result = new ArrayList<>();
                        try {
                            for (int i = 0; i < objects.length(); i++) {
                                result.add(wrapper.wrap(objects.getJSONObject(i)));
                            }
                        } catch (JSONException | ParseException e) {
                            error(e);
                            return;
                        }

                        callback.response(result.toArray(emptyArray()));
                    }
                }
            );
        }
    }

    public void listSleepEntries(int child_id, int offset, int count, RequestCallback<TimeEntry[]> callback) {
        new GenericTimelineRequest<TimeEntry>(TimeEntry.class).genericTimelineRequest(
            ACTIVITIES.SLEEP,
            child_id,
            offset,
            count,
            callback,
            o -> {
                String notes = o.optString("notes");
                return new TimeEntry(
                    ACTIVITIES.SLEEP,
                    o.getInt("id"),
                    parseNullOrDate(o, "start"),
                    parseNullOrDate(o, "end"),
                    notes
                );
            }
        );
    }

    public void listFeedingsEntries(int child_id, int offset, int count, RequestCallback<FeedingEntry[]> callback) {
        new GenericTimelineRequest<FeedingEntry>(FeedingEntry.class).genericTimelineRequest(
            ACTIVITIES.FEEDING,
            child_id,
            offset,
            count,
            callback,
            o -> {
                String notes = o.optString("notes");

                Constants.FeedingMethodEnum feedingMethod = null;
                Constants.FeedingTypeEnum feedingType = null;

                for (Constants.FeedingMethodEnum m : Constants.FeedingMethodEnum.values()) {
                    if (m.post_name.equals(o.getString("method"))) {
                        feedingMethod = m;
                    }
                }
                for (Constants.FeedingTypeEnum t : Constants.FeedingTypeEnum.values()) {
                    if (t.post_name.equals(o.getString("type"))) {
                        feedingType = t;
                    }
                }

                return new FeedingEntry(
                    ACTIVITIES.FEEDING,
                    o.getInt("id"),
                    parseNullOrDate(o, "start"),
                    parseNullOrDate(o, "end"),
                    notes,
                    feedingMethod,
                    feedingType
                );
            }
        );
    }

    public void listTummyTimeEntries(int child_id, int offset, int count, RequestCallback<TimeEntry[]> callback) {
        new GenericTimelineRequest<TimeEntry>(TimeEntry.class).genericTimelineRequest(
            ACTIVITIES.TUMMY_TIME,
            child_id,
            offset,
            count,
            callback,
            o -> {
                String notes = o.optString("milestone");
                return new TimeEntry(
                    ACTIVITIES.TUMMY_TIME,
                    o.getInt("id"),
                    parseNullOrDate(o, "start"),
                    parseNullOrDate(o, "end"),
                    notes
                );
            }
        );
    }

    public void listChangeEntries(int child_id, int offset, int count, RequestCallback<ChangeEntry[]> callback) {
        new GenericTimelineRequest<ChangeEntry>(ChangeEntry.class).genericTimelineRequest(
            EVENTS.CHANGE,
            child_id,
            offset,
            count,
            callback,
            o -> {
                String notes = o.optString("notes");
                return new ChangeEntry(
                    EVENTS.CHANGE,
                    o.getInt("id"),
                    parseNullOrDate(o, "time"),
                    parseNullOrDate(o, "time"),
                    notes,
                    o.getBoolean("wet"),
                    o.getBoolean("solid")
                );
            }
        );
    }

    public void removeTimelineEntry(TimeEntry entry, RequestCallback<Boolean> callback) {
        dispatchQuery(
            "DELETE",
            entry.getApiPath(),
            null,
            new RequestCallback<String>() {
                @Override
                public void error(@NotNull Exception error) {
                    callback.error(error);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            }
        );
    }

    public void updateTimelineEntry(
        @NotNull TimeEntry entry,
        @NotNull QueryValues values,
        @NotNull RequestCallback<TimeEntry> callback
    ) {
        String path = "api/" + entry.type + "/" + entry.typeId + "/";
        dispatchQuery(
            "PATCH",
            path,
            values.toJsonObject().toString(),
            new RequestCallback<String>() {
                @Override
                public void error(@NonNull Exception error) {
                    callback.error(error);
                }

                @Override
                public void response(String response) {
                    try {
                        JSONObject o = new JSONObject(response);
                        callback.response(TimeEntry.fromJsonObject(o, entry.type));
                    } catch (JSONException | ParseException e) {
                        callback.error(e);
                    }
                }
            }
        );
    }
}