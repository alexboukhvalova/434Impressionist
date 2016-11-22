package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;

/**
 * Created by jon on 3/20/2016.
 * Edited by Alex Boukhvalova on 11/20/2016 for 434Impressionist project.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();
    private VelocityTracker _vTracker = null;

    private int _alpha = 150;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.SimpleSquare;

    private int _savedPicNumber = 1;


    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //adapted from draw test code given from 434 class
        if(_offScreenCanvas != null) { //make sure canvas object was created
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            //"clear" the canvas by filling the whole area with a white rectangle
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        }

        invalidate(); //calling onDraw
    }

    /**
     * Saves the painting using the same style of saving image to gallery as used in MainActivity
     * for downloading the imaged
     */
    public void savePainting(Context context){
        String fileName = "Impressionist" + _savedPicNumber;
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);

            //turn bitmap into an image before it can be saved
            _offScreenBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));

            FileUtils.addImageToGallery(file.getAbsolutePath(), context);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *Fills in the background with an opaque pixelated version of the image that the user can trace
     * over
     */
    public void setTraceBackground(){
        //int width = _imageView.getWidth();
        //int height = _imageView.getHeight();

        //Get the rectangle border dimensions for the offScreenBitmap
        Rect bitRectangle = getBitmapPositionInsideImageView(_imageView);
        int width = bitRectangle.width();
        int height = bitRectangle.height();

        int colorAtPixelInImage;
        Bitmap imageViewBitmap = _imageView.getDrawingCache();

        //use these dimension for the loop in order to fill the border drawn for the painting
        int finalWidth = width + bitRectangle.left;
        int finalHeight = height + bitRectangle.top;

        //loop through rows and columns of the offScreenCanvas in order to fill it with the opaque
        //pixelated background
        for (int w = bitRectangle.left; w < (finalWidth - 50); w += 50) {
            for (int h = bitRectangle.top; h < (finalHeight - 50); h += 50) {
                //grab original pixel colors from painting
                colorAtPixelInImage = imageViewBitmap.getPixel(w, h);
                _paint.setColor(colorAtPixelInImage);
                _paint.setAlpha(100); //making the background more opque than the original image
                _offScreenCanvas.drawRect(w,h,w+50,h+50,_paint); //draw the 50x50 quares

                invalidate(); //call onDraw
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //grab where the user is touching the offScreenCanvas
        int x = (int)motionEvent.getX();
        int y = (int)motionEvent.getY();

        Bitmap imageViewBitmap = _imageView.getDrawingCache(); //original painting view
        int colorAtTouchPixelInImage = imageViewBitmap.getPixel(x, y);

        switch (motionEvent.getAction()) {

            case MotionEvent.ACTION_DOWN:
                if(_vTracker == null) {
                    // Get VelocityTracker object to track the users velocity of touch motion.
                    _vTracker = VelocityTracker.obtain();
                }
                else {
                    // Reset the velocity tracker.
                    _vTracker.clear();
                }
                // Take into account user's movement.
                _vTracker.addMovement(motionEvent);

                _paint.setColor(colorAtTouchPixelInImage);
                if(_brushType == BrushType.VelocitySquare) {
                    _offScreenCanvas.drawRect(x + _vTracker.getXVelocity(),y + _vTracker.getYVelocity(),x - _vTracker.getXVelocity(),y - _vTracker.getYVelocity(),_paint);
                } else if (_brushType == BrushType.Circle) {
                    _offScreenCanvas.drawCircle(x,y,10,_paint);
                } else if (_brushType == BrushType.SimpleSquare) {
                    _offScreenCanvas.drawRect(x + 10,y + 10,x - 10,y - 10,_paint);
                } else if (_brushType == BrushType.Line) {
                    _offScreenCanvas.drawRect(x + 50,y + 2,x,y,_paint);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                _vTracker.addMovement(motionEvent);
                _vTracker.computeCurrentVelocity(1000);
                //scale down the veolocity by dividing by 100. Otherwise the values would be too
                //large for the given offscreenbitmap
                float xVel = _vTracker.getXVelocity()/100;
                float yVel = _vTracker.getYVelocity()/100;


                _paint.setColor(colorAtTouchPixelInImage);
                if(_brushType == BrushType.VelocitySquare) {
                    _offScreenCanvas.drawRect(x + xVel,y + yVel,x - xVel,y - yVel,_paint);
                } else if (_brushType == BrushType.Circle) {
                    _offScreenCanvas.drawCircle(x,y,20,_paint);
                } else if (_brushType == BrushType.SimpleSquare) {
                    _offScreenCanvas.drawRect(x + 10,y + 10,x - 10,y - 10,_paint);
                } else if (_brushType == BrushType.Line) {
                    _offScreenCanvas.drawRect(x + 50,y + 2,x,y,_paint);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

