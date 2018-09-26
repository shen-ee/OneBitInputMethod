package com.example.shenyi.inputmethodalpha;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by shenyi on 2017/11/9.
 */

public class DBConnection extends Activity {

    public String find()

    {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH +
                "/" + DBManager.DB_NAME, null);
        String sql = "select * from num where descriptor1='shuzi'or descriptor2='fuhao'";
        Cursor c = db.rawQuery(sql, null);
        return "hahah";
    }
}
