package com.example.shenyi.inputmethodalpha;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.shenyi.inputmethodalpha.R.id.btn;
import static com.example.shenyi.inputmethodalpha.R.id.btn1;
import static com.example.shenyi.inputmethodalpha.R.id.btn2;
import static com.example.shenyi.inputmethodalpha.R.id.btn3;
import static com.example.shenyi.inputmethodalpha.R.id.btn4;
import static com.example.shenyi.inputmethodalpha.R.id.btn5;
import static com.example.shenyi.inputmethodalpha.R.id.btn6;


//import static com.example.shenyi.inputmethodalpha.R.id.s0;
//import static com.example.shenyi.inputmethodalpha.R.id.s1;
//import static com.example.shenyi.inputmethodalpha.R.id.s2;
//import static com.example.shenyi.inputmethodalpha.R.id.s3;
//import static com.example.shenyi.inputmethodalpha.R.id.s4;
//import static com.example.shenyi.inputmethodalpha.R.id.s5;
//import static com.example.shenyi.inputmethodalpha.R.id.s6;

/**
 * Created by shenyi on 2017/11/5.
 * Modified by lintong on 2017/12/19.
 * Modified by shangzhen on 2018/04/10
 */

public class InputMethodAlpha extends InputMethodService implements OnKeyboardActionListener {
    protected KeyboardView keyboardView; // 对应keyboard.xml中定义的KeyboardView
    protected Keyboard keyboard;         // 对应qwerty.xml中定义的Keyboard
    public MySQLiteHelper myHelper;
    public DBManager dbHelper;
    public SQLiteDatabase db;
    Button[] buttons = new Button[50];
    Button[] signals = new Button[50];
    EditText eT ;
    private int current_number = 6; // 当前闪动的按键编号
    private int level = 1;//当前所处的层级
    protected String current_consonant; // 当前的声母
    private int button_maxNumber = 6;
    private String[]  str1;//一个关于拼音的全局变量，在text上显示
    private static final String INNER_URL = "http://166.111.139.15:8081/submit_text_for_IME";

    public int periodTime = 1000;
    public String flashColor = "#00FF00";

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }

    @Override
    public View onCreateInputView() {
        // keyboard被创建后，将调用onCreateInputView函数
        dbHelper = new DBManager(getApplicationContext());
        dbHelper.openDatabase();
        dbHelper.closeDatabase();
        myHelper = new MySQLiteHelper(getApplicationContext(), DBManager.DB_PATH + "/" + DBManager.DB_NAME, null, 1);

        Timer ticker;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                current_number = current_number>=button_maxNumber?0:current_number+1;
                msg.what = current_number;
                handler.sendMessage(msg);
                if(periodTime!=1000)
                {
                    this.cancel();
                }
            }
        }, 1000, periodTime);

        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);  // 此处使用了keyboard.xml
        keyboard = new Keyboard(this, R.xml.qwerty);  // 此处使用了qwerty.xml
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);

        final RelativeLayout layout_bt = new RelativeLayout(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.button, null);//导入button.xml

        buttons[0] = view.findViewById(btn);
        buttons[1] = view.findViewById(btn1);
        buttons[2] = view.findViewById(btn2);
        buttons[3] = view.findViewById(btn3);
        buttons[4] = view.findViewById(btn4);
        buttons[5] = view.findViewById(btn5);
        buttons[6] = view.findViewById(btn6);

        eT = view.findViewById(R.id.eT);

        setFlash(6);//闪烁
        setText_mainMenu();

        //layout_bt.setOrientation(LinearLayout.HORIZONTAL);
        layout_bt.addView(view);

        //final LinearLay                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                out layout = new LinearLayout(this);
        //layout.setOrientation(LinearLayout.VERTICAL);
        final RelativeLayout layout = new RelativeLayout(this);
        keyboardView.setAlpha((float)0);//设置透明度
        keyboardView.setPreviewEnabled(false);//设置预览不可见
        layout_bt.setBackgroundColor(Color.parseColor("#FFFFFF"));
        layout.addView(layout_bt);
        layout.addView(keyboardView);


        return layout;
    }


    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            setFlash(msg.what);
        }
    };

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();//输入交互
        //ic.commitText("当前level："+String.valueOf(level)+"\n",1);//当前level反馈
        switch (primaryCode) {
            case Keyboard.KEYCODE_DONE:
                switch (level) {
                    case 666://光标的操作
                    {
                        cleanButton();
                        switch (current_number) {
                            case 0: //"返回"
                                level = 1;
                                setText_mainMenu();
                                break;
                            case 1: //"左移"
                                level = 666;
                                eT.setSelection(getLeftPosition(eT.getText().toString(),eT.getSelectionStart()));
                                setText_move();
                                break;
                            case 2: //"右移"
                                level = 666;
                                eT.setSelection(getRightPosition(eT.getText().toString(),eT.getSelectionStart()));
                                setText_move();
                                break;
                        }
                        break;
                    }
                    case 888://设置界面
                    {
                        cleanButton();
                        switch (current_number) {
                            case 0: //"返回"
                                level = 1;
                                setText_mainMenu();
                                break;
                            case 1: //"光标速度"
                                level = 8881;
                                setText_moveSpeed();
                                break;
                            case 2: //"光标颜色"
                                level = 8882;
                                setText_moveColor();
                                break;
                        }
                        break;
                    }
                    case 8881:
                    {
                        cleanButton();
                        switch (current_number) {
                            case 0: //"返回"
                                level = 1;
                                setText_mainMenu();
                                break;
                            case 1: //"慢"
                                level = 8881;
                                periodTime = 3000;
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Message msg = new Message();
                                        current_number = current_number>=button_maxNumber?0:current_number+1;
                                        msg.what = current_number;
                                        handler.sendMessage(msg);
                                        if(periodTime!=3000)
                                        {
                                            this.cancel();
                                        }
                                    }
                                }, 1000, periodTime);
                                setText_moveSpeed();
                                break;
                            case 2: //"中"
                                level = 8881;
                                periodTime = 2000;
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Message msg = new Message();
                                        current_number = current_number>=button_maxNumber?0:current_number+1;
                                        msg.what = current_number;
                                        handler.sendMessage(msg);
                                        if(periodTime!=2000)
                                        {
                                            this.cancel();
                                        }
                                    }
                                }, 1000, periodTime);
                                setText_moveSpeed();
                                break;
                            case 3: //"快"
                                level = 8881;
                                periodTime = 1000;
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Message msg = new Message();
                                        current_number = current_number>=button_maxNumber?0:current_number+1;
                                        msg.what = current_number;
                                        handler.sendMessage(msg);
                                        if(periodTime!=1000)
                                        {
                                            this.cancel();
                                        }
                                    }
                                }, 1000, periodTime);
                                setText_moveSpeed();
                                break;
                        }
                        break;
                    }
                    case 8882:
                    {
                        cleanButton();
                        switch (current_number) {
                            case 0: //"返回"
                                level = 1;
                                setText_mainMenu();
                                break;
                            case 1: //"红"
                                level = 8882;
                                flashColor = "#FF0000";
                                setText_moveColor();
                                break;
                            case 2: //"绿"
                                level = 8882;
                                flashColor = "#00FF00";
                                setText_moveColor();
                                break;
                            case 3: //"蓝"
                                level = 8882;
                                flashColor = "#0000FF";
                                setText_moveColor();
                                break;
                        }
                        break;
                    }
                    case 1: // 一级菜单选择声母集合
                    {
                        String[] strings = buttons[current_number].getText().toString().split("\n");
                        cleanButton();
                        switch (current_number) {
                            case 0:
                                level = 11; // 特殊的二级菜单
                                //format test
//                                eT.setText(queryFormat("我de_位置zai_哪li_?"));
//                                eT.setText(cleanFormat(queryFormat("我de_位置zai_哪li_?")));
                                //format test
                                //api test
                                //eT.setText("我de_位置zai_哪li_?");
//                                final String query = queryFormat("我de_wei_置zai_哪li_?");
//                                eT.setText(query);
//                                new Thread() {
//                                    public void run() {
//                                        JSONObject res = getData(query);
//                                    }
//                                }.start();
                                //api test
                                setText_subMenu();
                                break;
                            default:
                                level = 2;//普通二级菜单（选声母）
                                setButton0();
                                setNewText(strings, 1,0);
                                break;
                        }
                    }
                    break;
                    case 11: // 特殊的二级菜单
                    {
                        cleanButton();
                        switch (current_number) {
                            case 0: //"数符"
                                level = 111;
                                findCharacter();
                                break;
                            case 1: //"返回"
                                level = 1;
                                setText_mainMenu();
                                break;
                            case 2: //"删除"
                                level = 11;
                                //ic.deleteSurroundingText(1, 0);
                                int position = eT.getSelectionStart();
                                int newPosition = getLeftPosition(eT.getText().toString(),position);
                                if(!eT.getText().equals(""))
                                {
                                    String str_left = eT.getText().subSequence(0,newPosition).toString();
                                    String str_right = eT.getText().subSequence(position,eT.getText().length()).toString();
                                    eT.setText(str_left+str_right);
                                }
                                setText_subMenu();
                                eT.setSelection(newPosition);
                                break;
                            case 3: //"清空"
                                level = 11;
                                eT.setText("");
                                setText_subMenu();
                                break;
                            case 5://"光标"
                                level = 666;
                                setText_move();
                                break;
                            case 6://"设置"
                                level = 888;
                                setText_settings();
                                break;
                            case 4://"确认"
                                level =1;
                                final String query = queryFormat(eT.getText().toString());
                                //eT.setText(query);
                                new Thread() {
                                    public void run() {
                                        JSONObject res = getData(query);
                                    }
                                }.start();
                                //ic.commitText(eT.getText(),1);
                                //eT.setText("");
                                setText_mainMenu();
                                break;
                            default:
                                level = 1;
                                setText_mainMenu();
                                break;
                        }
                    }
                    break;
                    case 111: {//数符选择主界面
                        String[] str = buttons[current_number].getText().toString().split("\n");// 当前选中光标上的文字
                        cleanButton();
                        switch (current_number) {
                            case 0: //"数符"
                                level = 11;
                                setNewText(str, 0,0);
                                break;
                            default:
                                level = 1111;
                                setButton0();
                                setNewText(str, 1,0);
                                break;
                        }
                    }
                    break;
                    case 1111: {
                        String str1111 = buttons[current_number].getText().toString();
                        cleanButton();
                        switch (current_number) {
                            case 0: //"数符"
                                level = 11;
                                setText_subMenu();
                                break;
                            default://输入符号
                                level = 1;
                                ic.commitText(str1111, 1);
                                setText_mainMenu();
                                break;
                        }
                    }
                    break;
                    case 2: {//选择声母
                        String[] str = buttons[current_number].getText().toString().split("\n");
                        cleanButton();
                        switch (current_number) {
                            case 0://数符  返回  删除  换行  保存  退出
                                level = 11;
                                setText_subMenu();
                                break;
                            default://选中一个声母
                                level = 3;
                                current_consonant = str[0];
                                findByConsonant(current_consonant);
                                setButton0();
                                setButton_textsize(1,6,20);
                                break;
                        }
                    }
                    break;
                    case 3: {//选择拼音集合
                        String[] str = buttons[current_number].getText().toString().split("\n");
                        cleanButton();
                        switch (current_number) {
                            case 0://数符  返回  删除  换行  保存  退出
                                level = 11;
                                setText_subMenu();
                                break;
                            default://选中一个拼音
                                level = 4;
                                if (str.length==4)
                                {
                                    level = 3;
                                    setButton0();
                                    buttons[1].setText(str[0]+"\n"+str[1]);
                                    buttons[2].setText(str[2]+"\n"+str[3]);
                                    break;
                                }
                                if (str.length==5)
                                {
                                    level = 3;
                                    setButton0();
                                    buttons[1].setText(str[0]+"\n"+str[1]);
                                    buttons[2].setText(str[2]+"\n"+str[3]);
                                    buttons[3].setText(str[4]);
                                    break;
                                }
                                findByPinyin(str,str.length);//查拼音
                                setButton0();
                                setButton_textsize(1,6,35);
                                break;
                        }
                    }
                    break;
                    case 4:{//候选字集合
                        String[] str = buttons[current_number].getText().toString().split("\n");
                        str1 = buttons[current_number].getText().toString().split("\n");
                        cleanButton();
                        switch (current_number) {
                            case 0://数符  返回  删除  换行  保存  退出
                                level = 11;
                                setText_subMenu();
                                break;
                            default://选中一个候选集合
                                level = 5;
                                    setNewText(str,1,1);//需要去掉拼音时
                                    setButton0();
                                break;
                        }
                    }
                    break;

                    case 5://
                    {
                        String str = buttons[current_number].getText().toString();
                        cleanButton();
                        switch (current_number) {
                            case 0://数符  返回  删除  换行  保存  退出
                                level = 11;
                                setText_subMenu();
                                break;
                            default://选中一个拼音
                                level = 1;
                                //ic.commitText(str,1);
                                if(str.equals("✖️"))
                                {
                                    String originalStr = eT.getText().toString();
                                    if(originalStr.length()!= eT.getSelectionStart())
                                    {
                                        String left = originalStr.substring(0,eT.getSelectionStart())+str1[0]+"_";
                                        String right = originalStr.substring(eT.getSelectionStart(),originalStr.length());
                                        eT.setText(left+right);
                                        eT.setSelection(left.length());
                                    }else {
                                        eT.setText(originalStr + str1[0]+"_");
                                        eT.setSelection(eT.getText().length());
                                    }
                                }
                                else {
                                    String originalStr = eT.getText().toString();
                                    if(originalStr.length()!= eT.getSelectionStart())
                                    {
                                        String left = originalStr.substring(0,eT.getSelectionStart())+str;
                                        String right = originalStr.substring(eT.getSelectionStart(),originalStr.length());
                                        eT.setText(left+right);
                                        eT.setSelection(left.length());
                                    }else {
                                        eT.setText(originalStr + str);
                                        eT.setSelection(eT.getText().length());
                                    }
                                }
                                setText_mainMenu();
                                break;
                        }
                    }
                    break;
                }
                setFlash(0);
                setButton_maxNumber();
                break;
            default:
                char code = (char) primaryCode;
                ic.commitText(String.valueOf(code), 1);
        }
    }

    public void cleanButton() {//清除所有button的文字
        for (int i = 0; i < 7; i++) {
            buttons[i].setText("");
        }
    }

    public void setNewText(String[] string, int start,int mode) {//将string数组中的字符串赋给button，从button[start]开始
        int number = string.length;
        if(mode == 1)//当需要去掉一开始的拼音时
        {
            for (int i = start; i < start + number-1; i++) {
                buttons[i].setText(string[i - start+1]);
            }
        }else//普通模式
        {
            for (int i = start; i < start + number; i++) {
                buttons[i].setText(string[i - start]);
            }
        }
    }

    public void setExtraText(String[] string)
    {
        int number = string.length;
        int min = number/6;// 1
        int max = min+1;
        int longnum = number%6;
        int str_num = 0;

        for(int i=1;i<=longnum;i++)
        {
            String str="";
            for(int j=0;j<max;j++)
            {
                str+=string[str_num];
                str+="\n";
                str_num++;
            }
            buttons[i].setText(str);
        }
        for(int i=longnum+1;i<7;i++)
        {
            String str="";
            for(int j=0;j<min;j++)
            {
                str+=string[str_num];
                str+="\n";
                str_num++;
            }
            buttons[i].setText(str);
        }
    }

    public void setText_mainMenu()//设置主界面的key
    {
        setButton0();
        buttons[1].setText("b\np\nm\nr");
        buttons[2].setText("f\ng\nk\nh");
        buttons[3].setText("d\nt\nn\nl");
        buttons[4].setText("无声\nj\nq\nx");
        buttons[5].setText("zh\nch\ny\nsh");
        buttons[6].setText("z\nc\nw\ns");
        setButton_textsize(0,0,25);
        setButton_textsize(1,6,30);

    }

    public void setText_subMenu(){//第一个key的值
        buttons[0].setText("数符");
        buttons[1].setText("返回");
        buttons[2].setText("删除");
        buttons[3].setText("清空");
        buttons[4].setText("确认");
        buttons[5].setText("光标");
        buttons[6].setText("设置");
        setButton_textsize(0,6,30);
    }

    public void setText_move()
    {
        buttons[0].setText("返回");
        buttons[1].setText("左移");
        buttons[2].setText("右移");
        setButton_textsize(0,6,30);
    }

    public void setText_moveSpeed()
    {
        buttons[0].setText("返回");
        buttons[1].setText("慢");
        buttons[2].setText("中");
        buttons[3].setText("快");
        setButton_textsize(0,6,30);
    }

    public void setText_moveColor()
    {
        buttons[0].setText("返回");
        buttons[1].setText("红");
        buttons[2].setText("绿");
        buttons[3].setText("蓝");
        setButton_textsize(0,6,30);
    }

    public void setText_settings(){
        buttons[0].setText("返回");
        buttons[1].setText("光标速度");
        buttons[2].setText("光标颜色");
        setButton_textsize(0,6,30);
    }

    public void setButton0(){
        buttons[0].setText("数符\n返回\n删除\n清空\n确认\n光标\n设置");
    }

    public void setFlash(int number){
        final Animation anim = AnimationUtils.loadAnimation(InputMethodAlpha.this, R.anim.flash);
        for(int i=0;i<7;i++)
        {
                // buttons[i].clearAnimation();
                buttons[i].setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
        // buttons[number].setAnimation(anim);
        buttons[number].setBackgroundColor(Color.parseColor(flashColor));
        //signals[number].setBackgroundColor(Color.parseColor("#FF0000"));
        current_number = number;
    }

    public void setButton_maxNumber() {
        for(int i =0;i<7;i++)
        {
            if(buttons[i].getText()!="") {
                button_maxNumber = i;
            }
        }

    }

    public void setButton_blank(int start,int end,int blank_number) {
        int[] list = new int[6];
        int list_num = 0;
        for (int i = start; i <= end; i++) {
            if(!buttons[i].getText().equals(""))
            {
                list[list_num++]=i;
            }
        }
        for (int k = 0; k < list_num; k++) {
            int i = list[k];
            String a = "";
            int len = buttons[i].getText().toString().split("\n").length - 1;// '\n'数量
            if (len < blank_number) {
                int res = blank_number - len;
                for (int j = 0; j < res; j++) {
                    a = a + "\n";
                }
                buttons[i].setText(buttons[i].getText() + a);
            }
        }
    }

    public void setButton_textsize(int start ,int end, int size)
    {
        for(int i = start;i<=end;i++)
        {
            buttons[i].setTextSize(size);
        }
    }


    public void findCharacter() {
        db = myHelper.getReadableDatabase();
        String sql = "select * from num where descriptor1='shuzi'or descriptor2='fuhao'";
        Cursor c = db.rawQuery(sql, null);
        int index = 0;
        int number = 0;
        String[] ShuFu = new String[100];
        String[] Display_Button = new String[100];
        while (c.moveToNext()) {
            ShuFu[index] = c.getString(c.getColumnIndex("number"));
            String string = ShuFu[index];
            if (number < 6) {
                int index_temp = index;
                Display_Button[index_temp] = string;
                number++;
                index++;
            } else {
                int number_temp = number - (number / 6) * 6;
                Display_Button[number_temp] = Display_Button[number_temp] + "\n" + string;
                number++;
                index++;
            }
            for (int i = 1; i < 7; i++) {
                buttons[i].setText(Display_Button[i - 1]);
            }
            setButton0();
        }
    }

    public void findByConsonant(String consonant)
    {
        db = myHelper.getReadableDatabase();
        String sql = "select * from Demo1 where fuyin='" + consonant + "'";
        Cursor c1 = db.rawQuery(sql, null);
        int index1 = 0;
        int number1 = 0;
        String[] PINYIN = new String[100];
        String[] Display_Button1 = new String[100];
        while (c1.moveToNext()) {
            if (index1 == 0) {
                PINYIN[index1] = c1.getString(c1.getColumnIndex("pinyin"));
                Display_Button1[0] = PINYIN[index1];
                index1++;
                number1++;
            } else {
                PINYIN[index1] = c1.getString(c1.getColumnIndex("pinyin"));
                if (!PINYIN[index1].equals(PINYIN[index1 - 1])) {
                    if (number1 < 6) {
                        int index_temp = index1;
                        Display_Button1[index_temp] = PINYIN[index_temp];
                        number1++;
                        index1++;
                    } else {
                        int number_temp = number1 - (number1 / 6) * 6;
                        Display_Button1[number_temp] = Display_Button1[number_temp] + "\n" + PINYIN[index1];
                        number1++;
                        index1++;
                    }
                }
            }
        }
        for (int i = 1; i < 7; i++) {
            buttons[i].setText(Display_Button1[i - 1]);
        }
        c1.close();
    }

    public void findByPinyin(String[]string_separated,int num){
        if (num == 0) {
        } else if (num == 1) {
            String sql1 = "select * from Demo1 where pinyin='" + string_separated[0] +"'";
            Cursor c1 = db.rawQuery(sql1, null);
            String[] button = new String[100];//用来装每一个button里的字
            for(int i = 1;i<=4;i++)
            {
                button[i]="";
            }
            button[1]=button[2]=string_separated[0]+"\n";
            if(c1.moveToNext())
            {
                button[1]+=c1.getString(c1.getColumnIndex("hanzi"))+"\n";
                int number = 0;
                while(c1.moveToNext()){
                    if(number<4)
                        button[1]+=c1.getString(c1.getColumnIndex("hanzi"))+"\n";
                    if(number>=4)
                        button[2]+=c1.getString(c1.getColumnIndex("hanzi"))+"\n";//？？？3改成2
                    number ++;
                    if(number==9)
                        break;
                }
                if(button[2].equals(string_separated[0]+"\n"))
                {
                    button[2]="";
                }
            }
            for(int i = 1;i<=2;i++)
            {
                if(!button[i].equals(""))
                {
                    button[i]+="✖️";
                }
                buttons[i].setText(button[i]);
            }
            setButton_blank(1,4,5);
        }
            else if (num == 2) {
            String sql11 = "select * from Demo1 where pinyin='" + string_separated[0] +"'";
            String sql22 = "select * from Demo1 where pinyin='" + string_separated[1] +"'";
            Cursor c11 = db.rawQuery(sql11, null);
            Cursor c22 = db.rawQuery(sql22, null);
            String[] button = new String[100];
            for(int i = 1;i<=4;i++)
            {
                button[i]="";
            }
            button[1]=button[2]=string_separated[0]+"\n";
            button[3]=button[4]=string_separated[1]+"\n";
            if(c11.moveToNext())
            {
                button[1]+=c11.getString(c11.getColumnIndex("hanzi"))+"\n";
                int number = 0;
                while(c11.moveToNext()){
                    if(number<4)
                        button[1]+=c11.getString(c11.getColumnIndex("hanzi"))+"\n";
                    if(number>=4)
                        button[2]+=c11.getString(c11.getColumnIndex("hanzi"))+"\n";
                    number ++;
                    if(number==9)
                        break;
                }
                String[] str = button[4].split("\n");
                if(str.length<=2)
                {
                    button[2]="";
                }
            }
            if(c22.moveToNext())
            {
                int number = 0;
                button[3]+=c22.getString(c22.getColumnIndex("hanzi"))+"\n";
                while(c22.moveToNext()){
                    if(number<4)
                        button[3]+=c22.getString(c22.getColumnIndex("hanzi"))+"\n";
                    if(number>=5)
                        button[4]+=c22.getString(c22.getColumnIndex("hanzi"))+"\n";
                    number ++;
                    if(number==9)
                        break;
                }
                String[] str = button[4].split("\n");
                if(str.length<=2)//删除按键上没有字或者只有一个字的键
                {
                    button[4]="";
                }
            }
            if(button[2].equals("")){
                button[2]=button[3];
                button[3]=button[4];
                button[4]="";
            }
            for(int i = 1;i<=4;i++)
            {
                if(!button[i].equals(""))
                {
                    button[i]+="✖️";
                }
                buttons[i].setText(button[i]);
            }
            setButton_blank(1,4,5);
        } else if (num == 3) {
            String sql11 = "select * from Demo1 where pinyin='" + string_separated[0] + "'";
            String sql22 = "select * from Demo1 where pinyin='" + string_separated[1] + "'";
            String sql33 = "select * from Demo1 where pinyin='" + string_separated[2] + "'";
            Cursor c11 = db.rawQuery(sql11, null);
            Cursor c22 = db.rawQuery(sql22, null);
            Cursor c33 = db.rawQuery(sql33, null);
            String[] button = new String[100];
            for(int i = 1;i<=6;i++)
            {
                button[i]="";
            }
            button[1]=button[2]=string_separated[0]+"\n";
            button[3]=button[4]=string_separated[1]+"\n";
            button[5]=button[6]=string_separated[2]+"\n";
            if(c11.moveToNext())
            {
                int number = 0;
                button[1]+=c11.getString(c11.getColumnIndex("hanzi"))+"\n";
                while(c11.moveToNext()){
                    if(number<4)
                        button[1]+=c11.getString(c11.getColumnIndex("hanzi"))+"\n";
                    if(number>=4)
                        button[2]+=c11.getString(c11.getColumnIndex("hanzi"))+"\n";
                    number ++;
                    if(number==9)
                        break;
                }
                String[] str = button[2].split("\n");
                if(str.length<=2)//删除按键上没有字或者只有一个字的键
                {
                    button[2]="";
                }
            }
            if(c22.moveToNext())
            {
                int number = 0;
                button[3]+=c22.getString(c22.getColumnIndex("hanzi"))+"\n";
                while(c22.moveToNext()){
                    if(number<4)
                        button[3]+=c22.getString(c22.getColumnIndex("hanzi"))+"\n";
                    if(number>=5)
                        button[4]+=c22.getString(c22.getColumnIndex("hanzi"))+"\n";
                    number ++;
                    if(number==9)
                        break;
                }
                String[] str = button[4].split("\n");
                if(str.length<=2)//删除按键上没有字或者只有一个字的键
                {
                    button[4]="";
                }
            }
            if(c33.moveToNext())
            {
                int number = 0;
                button[5]+=c33.getString(c33.getColumnIndex("hanzi"))+"\n";
                while(c33.moveToNext()&&number != 9){
                    if(number<4)
                        button[5]+=c33.getString(c33.getColumnIndex("hanzi"))+"\n";
                    if(number>=4)
                        button[6]+=c33.getString(c33.getColumnIndex("hanzi"))+"\n";
                    number ++;
                }
                String[] str = button[6].split("\n");
                if(str.length<=2)//删除按键上没有字或者只有一个字的键
                {
                    button[6]="";
                }
            }
            for(int i=1;i<5;i++){
                int a=i;
                if(button[a].equals("")){//消除空白
                    if(button[a+1].equals("")){
                        button[a]=button[a+2];
                        button[a+2]="";
                    }
                    else{
                        button[a]=button[a+1];
                        button[a+1]="";
                    }
                }
            }
            for(int i = 1;i<=6;i++)
            {
                if(!button[i].equals(""))
                {
                    button[i]+="✖️";
                }
                buttons[i].setText(button[i]);
            }
            setButton_blank(1,6,5);
        }
    }

    public String queryFormat(String oristr) {
        StringBuilder newstr = new StringBuilder("");
        int last_char = -1; // 0表示中文符号 1表示英文符号
        for (int i = 0; i < oristr.length(); i++) {
            char c=oristr.charAt(i);
            if(c<='z'&&c>='a') //字母
            {
                if(last_char==0||last_char==-1)
                {
                    newstr.append("{");
                }
                newstr.append(c);

                last_char = 1;
            }else{//非字母
                newstr.append(c);
                if(c == '_')
                {
                    newstr.append("_}");
                }

                last_char = 0;
            }
        }
        return newstr.toString();
    }

    public String cleanFormat(String oristr) {
        StringBuilder newstr = new StringBuilder("");
        for (int i = 0; i < oristr.length(); i++) {
            char c=oristr.charAt(i);
            if((!(c<='z'&&c>='a'))&&(!(c=='_'||c=='}'||c=='{'))) //字母
            {
                newstr.append(c);
            }
        }
        return newstr.toString();
    }

    public  JSONObject getData(String imestr) {
        List<NameValuePair> list = new ArrayList<>();
        list.add(new BasicNameValuePair("text", imestr));
        return doPost(list);
    }
    @Nullable
    public  JSONObject doPost(List<NameValuePair> paramList) {

        // 1.创建请求对象
        HttpPost httpPost = new HttpPost(INNER_URL);
        // post请求方式数据放在实体类中
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(paramList, HTTP.UTF_8);
            httpPost.setEntity(entity);
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        // 2.创建客户端对象
        HttpClient httpClient = new DefaultHttpClient();
        // 3.客户端带着请求对象请求服务器端
        try {
            // 服务器端返回请求的数据
            //eT.setText("yeah2.9");
            HttpResponse httpResponse = httpClient.execute(httpPost);
           // eT.setText("yeah3");
            // 解析请求返回的数据
            if (httpResponse != null
                    && httpResponse.getStatusLine().getStatusCode() == 200) {
                final String element = EntityUtils.toString(httpResponse.getEntity(),
                        HTTP.UTF_8);
                if (element.startsWith("{")) {

                    eT.post(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                JSONObject res = new JSONObject(element);
                                int num = res.getJSONArray("result").length();
                                String oristr = cleanFormat(eT.getText().toString());
                                StringBuffer newstr = new StringBuffer();

                                newstr.append(oristr);

                                for(int i = 0 ;i<num;i++)
                                {
                                    String finalchar = getCandidate(res,i);//自动生成的文字
                                    int position = getPosition(res,i);
                                    newstr.insert(position,finalchar);
                                }
                                eT.setText(newstr.toString());
                                eT.setSelection(eT.getText().length());
                                getCurrentInputConnection().commitText(eT.getText(),1);
                                eT.setText("");
                            }catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });


                    try {
                        return new JSONObject(element);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getCandidate(JSONObject res,int index)
    {
        String finalchar = "";
        try{
            JSONArray candidates = res.getJSONArray("result");
            JSONObject candidate = candidates.getJSONObject(index);
            JSONArray choice = candidate.getJSONArray("candidates");
            Object fin = choice.get(0);
            finalchar = fin.toString().substring(2,3);
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return finalchar;
    }

    public int getPosition(JSONObject res,int index)
    {
        int finalposition = -1;
        try{
            JSONArray candidates = res.getJSONArray("result");
            JSONObject candidate = candidates.getJSONObject(index);
            Object position = candidate.get("position");
            finalposition = Integer.parseInt(position.toString());
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return finalposition;
    }

    public int getLeftPosition(String str , int selectionPosition)
    {
        String tmp_str = str.substring(0,selectionPosition);
        int currentPosition = selectionPosition-1;
        if(tmp_str.charAt(currentPosition)=='_')//unsure selectionPosition-1
        {
            currentPosition -= 1;
            while(tmp_str.charAt(currentPosition)<='z'&&tmp_str.charAt(currentPosition)>='a')
            {
                currentPosition -= 1;
                if(currentPosition == -1)
                {
                    return 0;
                }
            }
            return currentPosition + 1;
        }else {
            return selectionPosition-1;
        }
    }
    public int getRightPosition(String str , int selectionPosition)
    {
        int currentPosition = selectionPosition;
        if(str.length()>selectionPosition){
            if(str.charAt(currentPosition)<= 'z'&& str.charAt(currentPosition)>='a')//
            {
                currentPosition += 1;
                while(str.charAt(currentPosition)!='_')
                {
                    currentPosition += 1;
                }
                return currentPosition + 1;
            }else {
                return selectionPosition + 1;
            }
        }
        return selectionPosition;
    }

}
