package ucp.ntm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    //Button buttonServer = (Button)findViewById(R.id.btnServer);
    //Button buttonClient = (Button)findViewById(R.id.btnClient);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    /** Called when user taps server button **/
    public void chooseServer(View view){
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }

    /** Called when user taps client button **/
    public void chooseClient(View view){
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }
}
