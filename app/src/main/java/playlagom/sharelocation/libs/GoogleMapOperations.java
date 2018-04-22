package playlagom.sharelocation.libs;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by User on 4/20/2018.
 */

public class GoogleMapOperations {
    // TODO: 4/20/2018 STRAT IMPLEMENTING from here when DisplayActivity.java's line of code exceeds >= 1k

    // SET Margin dynamically  for any view (generic way)
    // SUPPORT: https://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
    public static void setMargins (View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }
}
