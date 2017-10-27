package ucp.ntm;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ServerActivity extends AppCompatActivity {

    // Text field uri
    private EditText textField ;

    // button to show progress dialog
    private Button btnShowProgress;

    // Progress Dialog
    //private ProgressBar pDialog;
    private ProgressDialog pDialog;

    // Attributs liste des fichiers
    private ListView files_list ;
    private View previousSelection ;
    private int nb_videos = 0 ;
    private ArrayList<File> video_files ;
    private ArrayList<String> videos_names = new ArrayList<>();
    private ArrayAdapter<String> adapter ;

    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;

    // File url to download
    private static String file_url = "https://ia800201.us.archive.org/22/items/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";

    ImageView my_image ;
    VideoView my_video ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("ServerActivity", "Je suis la");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MenuActivity.EXTRA_MESSAGE);

        // text field
        textField = (EditText) findViewById(R.id.field);
        // show progress bar button
        btnShowProgress = (Button) findViewById(R.id.btnProgressBar);

        // Image view to show image after downloading
        files_list = (ListView) findViewById(R.id.my_list);
        files_list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        /**
         * Default text field
         */
        textField.setText("https://ia800201.us.archive.org/22/items/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4", TextView.BufferType.EDITABLE);

        /**
         * Setting video lists
         */
        video_files = setFiles_list();
        nb_videos = video_files.size() ;
        for(int i = 0 ; i < nb_videos ; i++){
            videos_names.add(video_files.get(i).getName());
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, videos_names);
        files_list.setAdapter(adapter);
        previousSelection = null;

        /**
         * Show Progress bar click event
         * */
        btnShowProgress.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // starting new Async Task
                file_url = textField.getText().toString();
                Log.d("ServerActivity", "file_url = " + textField.getText().toString());
                new DownloadFileFromURL().execute(file_url);
            }
        });

        /**
         * List view event
         */
        files_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                if(previousSelection != null){
                    previousSelection.setBackgroundColor(Color.TRANSPARENT);
                }

                view.setBackgroundColor(Color.BLUE);
                Log.d("ServerActivity", (String)adapter.getItemAtPosition(position));
                previousSelection = view ;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true ;
    }

    /**
     * Showing Dialog
     * */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    protected ArrayList<File> setFiles_list(){
        //Log.d("ServerActivity", "Je vais lister les fichiers !");
        File download_dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        File[] files = download_dir.listFiles();
        ArrayList<File> mp4_files = new ArrayList<>();
        int nb_mp4_files = 0 ;

        for(int i = 0 ; i < files.length ; i++){
            //Log.d("ServerActivity", "Je liste les fichiers, fichier actuel : " + i + " nom : " +files[i].getName());

            if(files[i].getName().endsWith(".mp4")){
                //Log.d("ServerActivity", "J'ai trouvé un mp4 " + i + " nom : " +files[i].getName());
                mp4_files.add(files[i]);
                nb_mp4_files++ ;
            }
        }
        //Log.d("ServerActivity", "J'ai listé les fichiers mp4, j'en ai trouvé : " + nb_mp4_files);
        return mp4_files;
    }

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            Log.d("ServerActivity", "Before try");
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();
                String file_name = f_url[0].substring(f_url[0].lastIndexOf("/") +1);
                Log.d("ServerActivity", "File name = " + file_name);
                Log.d("ServerActivity", "lenghtOfFile = " +lenghtOfFile);

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output ;
                // if there is an sd card we save on it
                if(Environment.getExternalStorageState().equals("MEDIA_MOUNTED")) {
                    Log.d("ServerActivity", "Il y a une SD");
                    output = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + file_name);
                }
                else {
                    Log.d("ServerActivity", "Il n'y a pas de SD");
                    Log.d("ServerActivity", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + file_name);

                    output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+ "/" + file_name);
                }

                Log.d("ServerActivity", "After save");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(""+(int)((total*100)/lenghtOfFile));

                    //Log.d("ServerActivity", "Saving file");

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }


            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
            //Log.d("ServerActivity", "onProgressUpdate");
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(progress_bar_type);

            // Relisting of files in Download folder.
            video_files = setFiles_list();
            nb_videos = video_files.size() ;
            videos_names.clear();
            for(int i = 0 ; i < nb_videos ; i++){
                videos_names.add(video_files.get(i).getName());
            }

            if(previousSelection != null){
                previousSelection.setBackgroundColor(Color.TRANSPARENT);
            }
            adapter.notifyDataSetChanged();

            /*String imagePath ;
            // Displaying downloaded image into image view

            // Reading image path from sdcard
            if(Environment.getExternalStorageState().equals("MEDIA_MOUNTED")) {
                Log.d("ServerActivity", "Il y a une SD");
                imagePath = Environment.getExternalStorageDirectory().toString() + "/downloadedfile.jpg";
            }
            else {
                Log.d("ServerActivity", "Il n'y a pas de SD");
                Log.d("ServerActivity", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/newFile.mp4");

                imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/newFile.mp4" ;
            }

            // setting downloaded into image view
            //my_image.setImageDrawable(Drawable.createFromPath(imagePath));
            //my_video.setVideoPath(imagePath);*/

        }
    }
}
