package com.hnzly.myapplicationtest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class ListenActivity extends Activity implements MediaPlayer.OnPreparedListener,
                                                        MediaController.MediaPlayerControl {

    private MediaPlayer mMediaPlayer;
    private MediaController mController;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen);

        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the message from the intent
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Create the text view
        TextView textView = (TextView) findViewById(R.id.podcast_listen_name);
        textView.setTextSize(30);
        textView.setText(message);

        List<MainActivity.podcast> podcastList = MainActivity.retrievePodcastDats();
        MainActivity.podcast foundPodcast = null;
        for (int i=0; i< podcastList.size()-1; i++) {
            if(message.equals(podcastList.get(i).nm)) {
                foundPodcast = podcastList.get(i);
                break;
            }
        }

        String episodeUrl = "";
        int imgId = -1;
        String episodeDesc = "";
        if (foundPodcast != null) {
            //grab the most recent episode for now
            episodeUrl = foundPodcast.episodes.get(0).episode_url;
            episodeDesc = foundPodcast.episodes.get(0).desc;
            imgId = foundPodcast.imgId;
        } else {
            //oops!  We didn't find the podcast?  Add handling for this
        }

        ImageView imageView = (ImageView) findViewById(R.id.podcast_image);
        imageView.setImageDrawable(ResizeImage(imgId, 2));

        TextView descTxt = (TextView) findViewById(R.id.podcast_episode_desc);
        descTxt.setText(episodeDesc);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mController = new MediaController(this){
            @Override
            public void hide() {
                this.show(0);
            }

            @Override
            public void setMediaPlayer(MediaPlayerControl player) {
                super.setMediaPlayer(player);
                this.show();
            }
        };

        try{
            mMediaPlayer.setDataSource(episodeUrl);
            mMediaPlayer.prepare(); // might take long! (for buffering, etc)
            mMediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.listen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mController.hide();
        mMediaPlayer.stop();
        mMediaPlayer.release();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //the MediaController will hide after 3 seconds - tap the screen to make it appear again
        mController.show();
        return false;
    }

    //--MediaPlayerControl methods----------------------------------------------------
    public void start() {
        mMediaPlayer.start();
    }

    public void pause() {
        mMediaPlayer.pause();
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public void seekTo(int i) {
        mMediaPlayer.seekTo(i);
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        return 0;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canSeekBackward() {
        return true;
    }

    public boolean canSeekForward() {
        return true;
    }
    //--------------------------------------------------------------------------------

    public void onPrepared(MediaPlayer mediaPlayer) {
        mController.setMediaPlayer(this);
        mController.setAnchorView(findViewById(R.id.listen_container));

        handler.post(new Runnable() {
            public void run() {
                mController.setEnabled(true);
                mController.show();
            }
        });
    }
    /************************ Calculations for Image Sizing *********************************/
    private Drawable ResizeImage (int imageID, double imgScaleRatio) {
        //Get device dimensions
        Display display = getWindowManager().getDefaultDisplay();
        double deviceWidth = display.getWidth();

        BitmapDrawable bd=(BitmapDrawable) this.getResources().getDrawable(imageID);
        double imageHeight = bd.getBitmap().getHeight();
        double imageWidth = bd.getBitmap().getWidth();

        double ratio = (deviceWidth / imageWidth) * imgScaleRatio; //adding divisor to downsize image
        int newImageHeight = (int) (imageHeight * ratio);
        int newImageWidth = (int) (imageWidth * ratio);

        Bitmap bMap = BitmapFactory.decodeResource(getResources(), imageID);

        return new BitmapDrawable(this.getResources(),getResizedBitmap(bMap,newImageHeight,newImageWidth));
    }

    /************************ Resize Bitmap *********************************/
    private static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {

        int width = bm.getWidth();
        int height = bm.getHeight();

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // create a matrix for the manipulation
        Matrix matrix = new Matrix();

        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }
}
