package com.xhr.FileUpDown.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.xhr.FileUpDown.download.ThreadInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xhrong on 15-10-29.
 */
public class ThreadInfoDao extends AbstractDao<ThreadInfo> {

    private static final String TABLE_NAME = ThreadInfo.class.getSimpleName();

    public ThreadInfoDao(Context context) {
        super(context);
    }

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(_id integer primary key autoincrement, id integer,did text, url text, start long, end long, finished long)");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("drop table if exists " + TABLE_NAME);
    }

    public void insert(ThreadInfo info) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into "
                        + TABLE_NAME
                        + "(id,did, url, start, end, finished) values(?, ?, ?, ?, ?, ?)",
                new Object[]{info.getId(), info.getDownloadId(), info.getUrl(), info.getStart(), info.getEnd(), info.getFinished()}
        );
    }

    public void delete(String downloadId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from "
                        + TABLE_NAME
                        + " where did = ?",
                new Object[]{downloadId}
        );
    }

    public void update(String downloadId, int threadId, long finished) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("update "
                        + TABLE_NAME
                        + " set finished = ?"
                        + " where did = ? and id = ? ",
                new Object[]{finished, downloadId, threadId}
        );
    }

    public List<ThreadInfo> getThreadInfos(String downloadId) {
        List<ThreadInfo> list = new ArrayList<ThreadInfo>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from "
                        + TABLE_NAME
                        + " where did = ?",
                new String[]{downloadId}
        );
        while (cursor.moveToNext()) {
            ThreadInfo info = new ThreadInfo();
            info.setId(cursor.getInt(cursor.getColumnIndex("id")));
            info.setDownloadId(cursor.getString(cursor.getColumnIndex("did")));
            info.setUrl(cursor.getString(cursor.getColumnIndex("url")));
            info.setEnd(cursor.getLong(cursor.getColumnIndex("end")));
            info.setStart(cursor.getLong(cursor.getColumnIndex("start")));
            info.setFinished(cursor.getLong(cursor.getColumnIndex("finished")));
            list.add(info);
        }
        cursor.close();
        return list;
    }

    public boolean exists(String downloadId, int threadId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from "
                        + TABLE_NAME
                        + " where did = ? and id = ?",
                new String[]{downloadId, threadId + ""}
        );
        boolean isExists = cursor.moveToNext();
        cursor.close();
        return isExists;
    }
}
