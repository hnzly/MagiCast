package com.hnzly.myapplicationtest;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;
import android.graphics.Point;
import android.view.inputmethod.InputMethodManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    public final static String EXTRA_MESSAGE = "com.hnzly.myapplicationtest.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        attachListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void attachListeners(){
        // Attach to input box
        AutoCompleteTextView editText = (AutoCompleteTextView) findViewById(R.id.autocomplete_srch_inp);
        editText.setOnEditorActionListener(new AutoCompleteTextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    handled = true;
                }
                return handled;
            }
        });
        editText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                performSearch();
            }
        });

        // Get a reference to the AutoCompleteTextView in the layout
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.autocomplete_srch_inp);

        // Get source data
        List<String> podcastNames = getPodcastNames(retrievePodcastDats());

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, podcastNames);
        textView.setAdapter(adapter);

        // Attach to other elements
    }

    public void performSearch(){

        //get input argument
        AutoCompleteTextView editText = (AutoCompleteTextView) findViewById(R.id.autocomplete_srch_inp);

        // Get data
        List<podcast> podcastList = retrievePodcastDats();

        String selectedPodcastNm = editText.getText().toString();
        String podcastDesc = "Unknown";
        int imgId = 0;

        podcast foundPodcast = null;
        for (int i=0; i< podcastList.size()-1; i++) {
            if(selectedPodcastNm.equals(podcastList.get(i).nm)) {
                foundPodcast = podcastList.get(i);
                break;
            }
        }

        if (foundPodcast != null) {
            imgId = foundPodcast.imgId;
            podcastDesc = foundPodcast.desc;
        } else {
            //oops!! throw an execption?  or display Not Found gfx and language.
        }

        //get display stats
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        //collapse the keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        String htmlTxt = "<html><body style=\"text-align:justify\"> %s </body></Html>";
        WebView col1TxtWebView = (WebView) findViewById(R.id.col1_stringWebView);
        col1TxtWebView.setBackgroundColor(0x00000000);
        col1TxtWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE,null);
        col1TxtWebView.loadData(String.format(htmlTxt,podcastDesc),"text/html","utf-8");

        ImageView imageView = (ImageView) findViewById(R.id.col1_image);
        imageView.setImageDrawable(ResizeImage(imgId, 1.5 ));

        Button listenButton = (Button) findViewById(R.id.col1_button);
        listenButton.setVisibility(View.VISIBLE);
    }
    /************************ Calculations for Image Sizing *********************************/
    private Drawable ResizeImage (int imageID, double imgScaleRatio) {
        //Get device dimensions
        Display display = getWindowManager().getDefaultDisplay();
        double deviceWidth = display.getWidth();

        BitmapDrawable bd=(BitmapDrawable) this.getResources().getDrawable(imageID);
        double imageHeight = bd.getBitmap().getHeight();
        double imageWidth = bd.getBitmap().getWidth();

        double ratio = (deviceWidth / imageWidth) / imgScaleRatio; //adding divisor to downsize image
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

    public void listen(View view) {
        Intent intent = new Intent(this, ListenActivity.class);
        AutoCompleteTextView editText = (AutoCompleteTextView) findViewById(R.id.autocomplete_srch_inp);
        String podcastTitle = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, podcastTitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    static class podcast {
        public int id;
        public String nm;
        public String desc;
        public List<episode> episodes = new ArrayList<episode>();
        public int imgId;
    }

    static class episode {
        public int eposide_id;
        public int podcast_id;
        public String episode_url;
        public String desc;
    }

    public static List<podcast> retrievePodcastDats () {
        List<podcast> podcastList = new ArrayList<podcast>();

        //Add new podcast1
        podcast newPodcast1 = new podcast();
        newPodcast1.id=100001;
        newPodcast1.nm="This American Life";
        newPodcast1.imgId=R.drawable.this_american_life;
        newPodcast1.desc="This American Life - This American Life is a weekly public radio show broadcast on more than 500 stations to about 2.1 million listeners. It is produced by Chicago Public Media, distributed by Public Radio International, and has won all of the major broadcasting awards. It is also often the most popular podcast in the country, with around one million people downloading each week.";

        episode newEpisode1 = new episode();
        newEpisode1.podcast_id=100001;
        newEpisode1.eposide_id=1;
        newEpisode1.desc="";
        newEpisode1.episode_url="http://www.podtrac.com/pts/redirect.mp3/podcast.thisamericanlife.org/podcast/206.mp3";
        newPodcast1.episodes.add(newEpisode1);

        podcastList.add(newPodcast1);
        //End add new podcast

        //Add new podcast2
        podcast newPodcast2 = new podcast();
        newPodcast2.id=100002;
        newPodcast2.nm="Freakonomics Radio";
        newPodcast2.imgId=R.drawable.freakonomics;
        newPodcast2.desc="Freakonomics Radio - In their books \"Freakonomics\" and \"SuperFreakonomics,\" Steven D. Levitt and Stephen J. Dubner explore \"the hidden side of everything,\" with stories about cheating schoolteachers, self-dealing real-estate agents, and crack-selling mama\'s boys. The Freakonomics Radio podcast, hosted by Dubner, carries on that tradition with weekly episodes. Prepare to be enlightened, engaged, perhaps enraged, and definitely surprised.";

        episode newEpisode2 = new episode();
        newEpisode2.podcast_id=newPodcast2.id;
        newEpisode2.eposide_id=1;
        newEpisode2.desc="Why attend college?";
        newEpisode2.episode_url="http://www.podtrac.com/pts/redirect.mp3/audio.wnyc.org/freakonomics_podcast/freakonomics_podcast073012.mp3";
        newPodcast2.episodes.add(newEpisode2);

        podcastList.add(newPodcast2);
        //End add new podcast

        //Add new podcast3
        podcast newPodcast3 = new podcast();
        newPodcast3.id=100003;
        newPodcast3.nm="Tech News Today";
        newPodcast3.imgId=R.drawable.tech_news_today;
        newPodcast3.desc="Tech News Today - Tech News Today explores the most important stories of the day in conversation with the world\'s leading journalists live each weekday at 10:00am Pacific, 1:00pm Eastern.";

        episode newEpisode3 = new episode();
        newEpisode3.podcast_id=newPodcast3.id;
        newEpisode3.eposide_id=1;
        newEpisode3.desc="";
        newEpisode3.episode_url=""; // FILL THIS IN LATER
        newPodcast3.episodes.add(newEpisode3);

        podcastList.add(newPodcast3);
        //End add new podcast

        //Add new podcast4
        podcast newPodcast4 = new podcast();
        newPodcast4.id=100004;
        newPodcast4.nm="NPR: Planet Money";
        newPodcast4.imgId=R.drawable.planet_money;
        newPodcast4.desc="NPR: Planet Money - Imagine you could call up a friend and say, \"Meet me at the bar and tell me what\'s going on with the economy.\" Now imagine that\'s actually a fun evening. That\'s what we\'re going for at Planet Money.";

        episode newEpisode4 = new episode();
        newEpisode4.podcast_id=newPodcast4.id;
        newEpisode4.eposide_id=1;
        newEpisode4.desc="";
        newEpisode4.episode_url=""; // FILL THIS IN LATER
        newPodcast4.episodes.add(newEpisode4);

        podcastList.add(newPodcast4);
        //End add new podcast

        //Add new podcast5
        podcast newPodcast5 = new podcast();
        newPodcast5.id=100005;
        newPodcast5.nm="Radiolab";
        newPodcast5.imgId=R.drawable.radiolab;
        newPodcast5.desc="Radiolab - Radiolab is a show about curiosity. Where sound illuminates ideas, and the boundaries blur between science, philosophy, and human experience.";

        episode newEpisode5 = new episode();
        newEpisode5.podcast_id=newPodcast5.id;
        newEpisode5.eposide_id=1;
        newEpisode5.desc="";
        newEpisode5.episode_url=""; // FILL THIS IN LATER
        newPodcast5.episodes.add(newEpisode5);

        podcastList.add(newPodcast5);
        //End add new podcast

        //Add new podcast6
        podcast newPodcast6 = new podcast();
        newPodcast6.id=100006;
        newPodcast6.nm="This Week In Tech";
        newPodcast6.imgId=R.drawable.twit;
        newPodcast6.desc="This Week In Tech - Your first podcast of the week is the last word in tech. Join the top tech pundits in a roundtable discussion of the latest trends in high tech.";

        episode newEpisode6 = new episode();
        newEpisode6.podcast_id=newPodcast6.id;
        newEpisode6.eposide_id=1;
        newEpisode6.desc="";
        newEpisode6.episode_url=""; // FILL THIS IN LATER
        newPodcast6.episodes.add(newEpisode6);

        podcastList.add(newPodcast6);
        //End add new podcast

        //Add new podcast7
        podcast newPodcast7 = new podcast();
        newPodcast7.id=100007;
        newPodcast7.nm="All About Android";
        newPodcast7.imgId=R.drawable.all_about_android;
        newPodcast7.desc="All About Android - All About Android delivers everything you want to know about Android each week--the biggest news, freshest hardware, best apps and geekiest how-to\'s--with Android enthusiasts Jason Howell, Gina Trapani, Ron Richards, and a variety of special guests along the way.";

        episode newEpisode7 = new episode();
        newEpisode7.podcast_id=newPodcast7.id;
        newEpisode7.eposide_id=1;
        newEpisode7.desc="";
        newEpisode7.episode_url=""; // FILL THIS IN LATER
        newPodcast7.episodes.add(newEpisode7);

        podcastList.add(newPodcast7);
        //End add new podcast

        //Add new podcast8
        podcast newPodcast8 = new podcast();
        newPodcast8.id=100008;
        newPodcast8.nm="The Moth";
        newPodcast8.imgId=R.drawable.the_moth;
        newPodcast8.desc="The Moth Podcast - The Moth is an acclaimed not-for-profit organization dedicated to the art and craft of storytelling. It is a celebration of both the raconteur, who breathes fire into true tales of ordinary life, and the storytelling novice, who has lived through something extraordinary and yearns to share it. At the center of each performance is, of course, the story – and The Moth’s directors work with each storyteller to find, shape and present it.";

        episode newEpisode8 = new episode();
        newEpisode8.podcast_id=newPodcast8.id;
        newEpisode8.eposide_id=1;
        newEpisode8.desc="";
        newEpisode8.episode_url=""; // FILL THIS IN LATER
        newPodcast8.episodes.add(newEpisode8);

        podcastList.add(newPodcast8);
        //End add new podcast

        return podcastList;
    }

    private List<String> getPodcastNames (List<podcast> inputList) {

        List<String> names = new ArrayList<String>();
        for (int i=0; i<inputList.size(); i++ ) {
             names.add(inputList.get(i).nm);
        }
        return names;
    }

    //NOT USED RIGHT NOW
    private void animate(final ImageView imageView, final Drawable images[], final int imageIndex, final boolean forever) {

        //imageView <-- The View which displays the images
        //images[] <-- Holds R references to the images to display
        //imageIndex <-- index of the first image to show in images[]
        //forever <-- If equals true then after the last image it starts all over again with the first image resulting in an infinite loop. You have been warned.

        int fadeInDuration = 1000; // Configure time values here
        int timeBetween = 300;
        int fadeOutDuration = 1000;

        imageView.setVisibility(View.INVISIBLE);    //Visible or invisible by default - this will apply when the animation ends
        imageView.setImageDrawable(images[imageIndex]);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); // add this
        fadeIn.setDuration(fadeInDuration);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); // and this
        fadeOut.setStartOffset(fadeInDuration + timeBetween);
        fadeOut.setDuration(fadeOutDuration);

        AnimationSet animation = new AnimationSet(false); // change to false
        animation.addAnimation(fadeIn);
        animation.addAnimation(fadeOut);
        animation.setRepeatCount(1);
        imageView.setAnimation(animation);

        animation.setAnimationListener(new AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                if (images.length - 1 > imageIndex) {
                    animate(imageView, images, imageIndex + 1,forever); //Calls itself until it gets to the end of the array
                }
                else {
                    if (forever == true){
                        animate(imageView, images, 0,forever);  //Calls itself to start the animation all over again in a loop if forever = true
                    }
                }
            }
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
            }
        });
    }
}
