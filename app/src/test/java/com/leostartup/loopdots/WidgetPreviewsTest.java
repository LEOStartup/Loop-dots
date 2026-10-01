package com.leostartup.loopdots;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {31, 35})
public class WidgetPreviewsTest {
    @Test public void everyProviderHasAnImageAndInflatablePreview() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        int[] providers = {R.xml.widget_small, R.xml.widget_compact, R.xml.widget_info,
            R.xml.widget_grid, R.xml.widget_wide};
        String ns = "http://schemas.android.com/apk/res/android";
        for (int i = 0; i < providers.length; i++) {
            try (XmlResourceParser xml = context.getResources().getXml(providers[i])) {
                while (xml.next() != XmlResourceParser.START_TAG) {}
                assertEquals(WidgetPreviews.LAYOUTS[i], xml.getAttributeResourceValue(ns, "previewLayout", 0));
                assertNotEquals(0, xml.getAttributeResourceValue(ns, "previewImage", 0));
            }
            View preview = new RemoteViews(context.getPackageName(), WidgetPreviews.LAYOUTS[i])
                .apply(context, new FrameLayout(context));
            ImageView image = preview.findViewById(R.id.preview_image);
            assertNotNull(image.getDrawable());
            for (int[] size : new int[][]{{140, 80}, {300, 180}}) {
                preview.measure(View.MeasureSpec.makeMeasureSpec(size[0], View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(size[1], View.MeasureSpec.EXACTLY));
                preview.layout(0, 0, size[0], size[1]);
                assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
            }
        }
    }
}
