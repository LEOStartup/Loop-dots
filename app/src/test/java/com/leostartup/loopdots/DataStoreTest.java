package com.leostartup.loopdots;
import android.content.Context;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class DataStoreTest {
    Context context;
    @Before public void seed(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("loop_dots",0).edit().clear().putString("habits_v2","[{\"id\":\"habit_default\",\"name\":\"Meditação\",\"color\":-65536,\"icon\":\"✓\"}]").putStringSet("done_v2_habit_default",new HashSet<>(Arrays.asList("2024-02-29","2025-12-31","2026-01-01"))).commit();}
    @Test public void migrationPreservesIdentityEveryDateAndOriginalPreferences() throws Exception {
        JSONObject root=DataStore.load(context),h=root.getJSONArray("habits").getJSONObject(0);
        assertEquals("habit_default",h.getString("id"));assertEquals("Meditação",h.getString("name"));assertEquals("#FF0000",h.getString("color"));assertEquals(3,h.getJSONObject("entries").length());assertEquals(1,h.getJSONObject("entries").getInt("2024-02-29"));
        assertEquals(3,context.getSharedPreferences("loop_dots",0).getStringSet("done_v2_habit_default",new HashSet<>()).size());assertNotNull(context.getSharedPreferences("loop_dots",0).getString("migration_snapshot_v3",null));
    }
    @Test public void migrationRunsOnceAndNewNotesSurviveReload() throws Exception {
        JSONObject root=DataStore.load(context);root.getJSONArray("habits").getJSONObject(0).getJSONObject("notes").put("2026-01-01","Texto da nota");DataStore.save(context,root.toString());assertEquals("Texto da nota",DataStore.load(context).getJSONArray("habits").getJSONObject(0).getJSONObject("notes").getString("2026-01-01"));
    }
    @Test public void invalidImportCannotReplaceSavedDocument() throws Exception {
        String before=DataStore.load(context).toString();try{DataStore.save(context,"{\"habits\":[{\"id\":\"bad'id\"}]}");fail("Expected validation failure");}catch(JSONException expected){}assertEquals(before,DataStore.load(context).toString());
    }
    @Test public void widgetChangesUseSameRecordsAsApplication() throws Exception {
        DataStore.load(context);DataStore.toggle(context,"habit_default","2025-12-31");assertFalse(DataStore.done(DataStore.habit(context,"habit_default"),"2025-12-31"));DataStore.toggle(context,"habit_default","2025-12-31");assertTrue(DataStore.done(DataStore.habit(context,"habit_default"),"2025-12-31"));assertEquals(3,DataStore.count(DataStore.habit(context,"habit_default")));
    }
}
