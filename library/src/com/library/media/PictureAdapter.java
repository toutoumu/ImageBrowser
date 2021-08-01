package com.library.media;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.library.glide.GlideApp;
import com.library.glide.GlideRequests;
import com.library.widget.DragFrameLayout;
import java.util.List;
import org.wordpress.passcodelock.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PictureAdapter extends PagerAdapter {
    private final List<Picture> pictures;
    private final PictureActivity mFragment;
    private final GlideRequests mGlideRequests;
    private final RequestOptions mRequestOptions;

    public PictureAdapter(PictureActivity activity, PictureActivity pictureFragment, List<Picture> pictures) {
        this.mFragment = pictureFragment;
        this.pictures = pictures;

        // Glide 4.x 加载数据
        mRequestOptions = RequestOptions.fitCenterTransform()
            .placeholder(R.drawable.ic_default_image_list)
            .override(3968, 3968);
        mGlideRequests = GlideApp.with(activity);
    }

    @Override
    public int getCount() {
        return pictures.size();
    }

    @NonNull
    @Override
    public View instantiateItem(ViewGroup container, int position) {
        DragFrameLayout layout = new DragFrameLayout(container.getContext());
        layout.setContainer(mFragment.bind.pager);
        layout.setImageView(mFragment.bind.smoothImageView);

        PhotoView photoView = new PhotoView(container.getContext());
        photoView.setMaximumScale(8);
        photoView.setOnViewTapListener((view, x, y) -> mFragment.toggleUI());
        photoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        layout.addView(photoView, MATCH_PARENT, MATCH_PARENT);
        container.addView(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mGlideRequests.load(pictures.get(position))
            // .thumbnail(0.1f) // 缩略图
            .apply(mRequestOptions)
            .into(photoView);

        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE; // 返回这个值才能用notify更新界面
    }
}