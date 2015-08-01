package edu.nyu.scps.stroker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


public class StrokeView extends View {
    static final String TABLE_NAME = "points";
    Helper helper;
    SQLiteDatabase db;
    int n;  //Number of strokes.  The strokes are numbered from 0 to n-1 inclusive.
    int selected;
    Path path = new Path();
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StrokeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE); //so onDraw doesn't have to call canvas.drawColor

        helper = new Helper(context, TABLE_NAME);
        db = helper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT max(stroke) FROM " + TABLE_NAME + ";", null);
        cursor.moveToFirst();
        int maxStrokeIndex = cursor.getColumnIndex("max(stroke)");
        if (cursor.isNull(maxStrokeIndex)) {
            n = 0;                                 //Table is empty.
        } else {
            n = cursor.getInt(maxStrokeIndex) + 1; //Table already contains n strokes.
        }
        selected = n - 1;
        cursor.close();

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);   //in dps

        selectedPaint.setColor(Color.RED);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(3f);   //in dps

        circlePaint.setColor(0x8080FF80);
        circlePaint.setStyle(Paint.Style.FILL);

        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ++n;
                        ++selected;
                    case MotionEvent.ACTION_MOVE:
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("stroke", n - 1); //n-1 is the most recent stroke.
                        contentValues.put("x", event.getX());
                        contentValues.put("y", event.getY());
                        db.insert(TABLE_NAME, null, contentValues);
                        invalidate();    //call onDraw
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    //Stroke number n-1 is the most recent stroke.

    public void previous() {
        if (selected > 0) {
            --selected;
        }
        invalidate();
    }

    public void next() {
        if (selected < n - 1) {
            ++selected;
        } else {
            Toast toast = Toast.makeText(getContext(), "Most recent stroke is ALREADY selected!", Toast.LENGTH_LONG);
            toast.show();
        }
        invalidate();
    }

    public void raise() {
        db.execSQL("update " + TABLE_NAME + " SET y = y - 20 WHERE stroke = " + selected + ";");
        invalidate();
    }

    public void lower() {
        db.execSQL("update " + TABLE_NAME + " SET y = y + 20 WHERE stroke = " + selected + ";");
        invalidate();
    }

    public void delete() {
        db.delete(TABLE_NAME, "stroke = ?", new String[] {String.valueOf(n - 1)});
        --n;
        invalidate();
    }

    public void delete_all() {
        db.delete(TABLE_NAME, null, null);    //Delete all rows from the table.
        n = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < n; ++i) {
            path.reset();
            Cursor cursor = db.query(
                    TABLE_NAME,
                    new String[] {"min(_id)", "x", "y"},
            "stroke = ?",
                    new String[] {String.valueOf(i)},
                    "stroke", //group by
                    null,     //having
                    null      //order by
            );

            if (cursor.moveToFirst()) {
                int xIndex = cursor.getColumnIndex("x");
                int yIndex = cursor.getColumnIndex("y");
                canvas.drawCircle(cursor.getFloat(xIndex), cursor.getFloat(yIndex), 20, circlePaint);
            }

            cursor.close();

            cursor = db.query(
                    TABLE_NAME,
                    new String[] {"x", "y"},
                    "stroke = ?",
                    new String[] {String.valueOf(i)},
                    null, //group by
                    null, //having
                    null  //order by
            );

            if (cursor.moveToFirst()) {
                int xIndex = cursor.getColumnIndex("x");
                int yIndex = cursor.getColumnIndex("y");
                path.moveTo(cursor.getFloat(xIndex), cursor.getFloat(yIndex));

                while (cursor.moveToNext()) {
                    path.lineTo(cursor.getFloat(xIndex), cursor.getFloat(yIndex));
                }
            }
            cursor.close();
            canvas.drawPath(path, i == selected ? selectedPaint : paint);
        }
    }
}
