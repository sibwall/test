package duress.ultimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

public class DefaultBrowserLink extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String url = "about:newtab";        
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));                
        startActivity(intent);
        
        finish();
    }
}
