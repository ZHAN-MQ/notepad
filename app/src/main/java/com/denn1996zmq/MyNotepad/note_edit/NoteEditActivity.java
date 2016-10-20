package com.denn1996zmq.MyNotepad.note_edit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.denn1996zmq.MyNotepad.ConfirmationDialogFragment;
import com.denn1996zmq.MyNotepad.NotepadApplication;
import com.denn1996zmq.MyNotepad.R;
import com.denn1996zmq.MyNotepad.notes.Note;
import com.denn1996zmq.MyNotepad.notes.NoteManager;

import java.lang.reflect.Field;
import java.util.Calendar;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NoteEditActivity extends ActionBarActivity implements ConfirmationDialogFragment.ConfirmationDialogListener {
    NotepadApplication application;
    Note currentNote;
    NoteManager noteManager;
    EditText textEdit;
    int mYear, mMonth, mDay, mHour, mMinute;

    /*Dialog IDs*/
    private final int DIALOG_DELETE = 1;
    private final int DIALOG_RESTORE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_edit);
        application = (NotepadApplication) this.getApplication();
        noteManager = application.getNoteManager();

        int id = getIntent().getExtras().getInt("noteId");
        currentNote = noteManager.getNoteById(id);

        textEdit = (EditText) findViewById(R.id.editText1);
        if (currentNote == null) {
            finish();
            return;
        }
        String s = currentNote.getText();
        mYear = currentNote.mYear;
        mMonth = currentNote.mMonth;
        mDay = currentNote.mDay;
        mHour = currentNote.mHour;
        mMinute = currentNote.mMinute;
        textEdit.setText(s);
        moveTextCaret();

        try {
            setupActionBar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setupActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public void onPause() {
        saveCurrentNote();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

	/*@Deprecated
	void showOverflowButton()
	{
		try
		{
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null)
			{
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}			
		}
		catch (Exception e)
		{
				
		}
	}*/

    void moveTextCaret() {
        textEdit.setSelection(textEdit.getText().toString().length());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_edit_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        saveOrDelete();
        super.onBackPressed();
    }
    @SuppressLint("ValidFragment")
    public class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            int hour = mHour;
            int minute = mMinute;
            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            mHour = hourOfDay;
            mMinute = minute;
        }
    }

    @SuppressLint("ValidFragment")
    public class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            int month, day, year;
            if(mYear<2000) {
                Calendar cal = Calendar.getInstance();
                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH);
                day = cal.get(Calendar.DATE);
            }else {
                year = mYear;
                month = mMonth;
                day = mDay;
            }
            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Do something with the date chosen by the user
            mYear = year;
            mMonth = month;
            mDay = day;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.saveNote:
                saveCurrentNote();
                break;
            case R.id.deleteItem:
                ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(this, getString(R.string.dialogDeleteNote), DIALOG_DELETE);
                dialog.show(getSupportFragmentManager(), "delete");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.dialogDeleteNote).setTitle(R.string.dialogDeleteNote);
                //AlertDialog dialog = builder.create();
                break;
            case R.id.revertChanges:
                ConfirmationDialogFragment d = ConfirmationDialogFragment.newInstance(this, getString(R.string.dialogRevertChanges), DIALOG_RESTORE);
                d.show(getSupportFragmentManager(), "restore");
                break;
            case R.id.settime:

                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getSupportFragmentManager(), "timePicker");


                DialogFragment newFragment1 = new DatePickerFragment();
                newFragment1.show(getSupportFragmentManager(), "datePicker");
                break;
            case R.id.shareNote:
                saveCurrentNote();
                currentNote.share(this);
                break;
            default:
                return false;
        }
        return true;
    }

    void saveOrDelete() {

        if (TextUtils.isEmpty(textEdit.getText())) {
            deleteCurrentNote();
        } else saveCurrentNote();
    }

    void deleteCurrentNote() {
        noteManager.deleteNote(currentNote);
        currentNote = null;
        Toast.makeText(getApplicationContext(), getString(R.string.toastNoteDeleted), Toast.LENGTH_SHORT).show();
    }

    void refreshNote() {
		/*Reverts any changes*/
        textEdit.setText(currentNote.getText());
        moveTextCaret();
    }

    private void saveCurrentNote() {
        try {
            String s = textEdit.getText().toString();
			/*if (currentNote.findChanges(s) == false)
			{
				Toast.makeText(getApplicationContext(), getString(R.string.toastNoteSaved), Toast.LENGTH_SHORT).show();
				return;
			}*/
            currentNote.setDateTime(mYear, mMonth, mDay, mHour, mMinute);
            currentNote.setText(s);
            currentNote.saveToFile(getApplicationContext());
            Toast.makeText(getApplicationContext(), getString(R.string.toastNoteSaved), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onYesClicked(DialogFragment dialog, Bundle bundle) {
        switch (bundle.getInt("dialogId")) {
            case DIALOG_DELETE:
                deleteCurrentNote();
                this.setResult(RESULT_OK);
                finish();
                break;

            case DIALOG_RESTORE:
                refreshNote();
                break;
        }
    }

    public void onNoClicked(DialogFragment dialog, Bundle bundle) {
    }
}
