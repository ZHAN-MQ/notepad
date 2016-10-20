package com.denn1996zmq.MyNotepad.notes;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;

import com.denn1996zmq.MyNotepad.NotepadApplication;
import com.denn1996zmq.MyNotepad.R;
import com.denn1996zmq.MyNotepad.note_list.LinkListDialog;
import com.denn1996zmq.MyNotepad.note_list.LinkListDialog.LinkListener;
import com.denn1996zmq.MyNotepad.note_list.NotepadListActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Note {
    private String text;
    String fileName = "";
    NoteManager noteManager = null;
    List<String> hyperlinks = new ArrayList<String>();
    public int mYear = 1990, mMonth, mDay, mHour, mMinute;

    public void setDateTime(int year, int month, int day, int hour, int minute) {
        mYear = year;
        mMonth = month;
        mDay = day;
        mHour = hour;
        mMinute = minute;
    }

    public static final int BUFFER_SIZE = 512;

    public Note(NoteManager noteManager) {
        this.noteManager = noteManager;
        this.text = "";
    }

    public Note(NoteManager noteManager, String content) {
        this(noteManager);
        if (content == null)
            setText("");
        else
            setText(content);
    }

    public Note(NoteManager noteManager, CharSequence content) {
        this(noteManager, content.toString());
    }

    public String getText() {
        return text;
    }

    private void findHyperlinks() {
        hyperlinks.clear();
        String[] words = this.text.toLowerCase(Locale.getDefault()).split("[\\s]");
        for (String word : words) {
            if (word.startsWith("http://") || word.startsWith("https://") ||
                    word.startsWith("www.") || word.endsWith(".com") || word.endsWith(".net") || word.endsWith(".cn")||word.endsWith(".org"))
            {
                if (word.startsWith("www.") || word.endsWith(".com") || word.endsWith(".net") || word.endsWith(".cn")||word.endsWith(".org"))
                    word = "http://" + word;
                hyperlinks.add(word.trim());
            }
        }
    }

    /**
     * Gets the list of hyperlinks found in the text
     */
    public List<String> getHyperlinks() {
        return hyperlinks;
    }

    public void setText(String t) {
        this.text = t;
        findHyperlinks();
    }

    public void appendText(String t) {
        setText(text.concat(t));
    }

    public String getStart(int numberCharacters, boolean ignoreNewline,
                           boolean addEllipsis) {
        String s = ignoreNewline ? text : text.trim();
        int end = Math.min(numberCharacters, s.length());
        if (ignoreNewline == false) {
            int nlPos = s.indexOf('\n');
            if (nlPos > 0) {
                end = Math.min(end, nlPos);
            }
        }
        return addEllipsis ? s.substring(0, end) + "�" : s.substring(0, end);
    }

    public String toString() {
        return getStart(100, false, false);
    }

    public int getID() {
        return noteManager.notes.indexOf(this);
    }

    void delete(Context context) {
        if (TextUtils.isEmpty(fileName) == false) {
            context.deleteFile(fileName);
        }
    }

    public void popupHyperlinks(final ActionBarActivity activity) {
        if (hyperlinks.isEmpty()) return;

        LinkListDialog dialog = new LinkListDialog();
        dialog.setHyperlinks(this.hyperlinks);
        dialog.setLinkListener(new LinkListener() {
            public void onLinkClicked(String url) {
                ((NotepadApplication) activity.getApplication()).openLink(url);
            }
        });
        dialog.show(activity.getSupportFragmentManager(), "Link selector");
    }

    public void copyToClipboard(NotepadApplication application) {
        application.setClipboardString(text);
    }

    public boolean findChanges(String newVersion) {
        //NotepadListActivity.currentNote.setDateTime(mYear, mMonth, mDay, mHour, mMinute);
        return (text.equals(newVersion) == false);
    }

    public void share(Context context) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(android.content.Intent.EXTRA_TITLE,
                context.getString(R.string.shareEntityName));
        share.putExtra(android.content.Intent.EXTRA_TEXT, text);
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(share,
                context.getString(R.string.sharePromptText)));
    }

    public void saveToFile(Context context) throws IOException {
        if (TextUtils.isEmpty(fileName) == true) {
            fileName = noteManager.generateFilename();
        }
        FileOutputStream file = context.openFileOutput(fileName,
                Context.MODE_PRIVATE);

        byte[] buffer = text.getBytes();
        file.write(buffer);
        file.flush();
        file.close();
        file = context.openFileOutput(fileName + "date",
                Context.MODE_PRIVATE);


        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:MM", Locale.CHINA);
        Calendar cal = Calendar.getInstance();
        cal.set(mYear, mMonth, mDay, mHour, mMinute);
        buffer = format.format(cal.getTime()).getBytes();
        file.write(buffer);
        file.flush();
        file.close();
    }

    /**
     * Loads a note from file
     *
     * @return
     * @throws IOException
     */
    public static Note newFromFile(NoteManager noteManager, Context context,
                                   String filename) throws IOException, ParseException {
        FileInputStream inputFileStream = context.openFileInput(filename);
        StringBuilder stringBuilder = new StringBuilder();
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = inputFileStream.read(buffer)) > 0) {
            String line = new String(buffer, 0, len);
            stringBuilder.append(line);

            buffer = new byte[Note.BUFFER_SIZE];
        }
        Note n = new Note(noteManager, stringBuilder.toString().trim());
        inputFileStream = context.openFileInput(filename + "date");
        buffer = new byte[BUFFER_SIZE];
        while ((len = inputFileStream.read(buffer)) > 0) {
            String line = new String(buffer, 0, len);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:MM", Locale.CHINA);
            Calendar cal = Calendar.getInstance();
            cal.setTime(format.parse(line));
            n.setDateTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)
                    , cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        }


        n.fileName = filename;

        inputFileStream.close();

        return n;
    }

    public static Note newFromClipboard(NoteManager noteManager, NotepadApplication application) {
        CharSequence string = application.getClipboardString();
        if (string == null) return null;
        return new Note(noteManager, string);
    }
}
