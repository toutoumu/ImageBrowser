package com.image.browser;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.library.widget.DragFrameLayout;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PictureAdapter extends PagerAdapter {
    private final List<Picture> pictures;
    private final PictureActivity mActivity;
    private final RequestOptions mRequestOptions;

    public PictureAdapter(PictureActivity pictureFragment, List<Picture> pictures) {
        this.mActivity = pictureFragment;
        this.pictures = pictures;

        // Glide 4.x 加载数据
        mRequestOptions = RequestOptions.fitCenterTransform()
            // .placeholder(R.drawable.transparent_drawable)
            // .placeholder(R.drawable.ic_default_image)
            // .placeholder(new GlidePlaceholderDrawable(mActivity.getResources(), R.drawable.ic_default_image))
            .override(3968, 3968);
    }

    @Override
    public int getCount() {
        return pictures.size();
    }

    @NonNull
    @Override
    public View instantiateItem(ViewGroup container, int position) {
        DragFrameLayout layout = new DragFrameLayout(container.getContext());
        layout.setViewPager(mActivity.bind.pager);
        layout.setImageView(mActivity.bind.smoothImageView);

        PhotoView photoView = new PhotoView(container.getContext());
        photoView.setMaximumScale(10);
        photoView.setMediumScale(4);
        photoView.setOnViewTapListener((view, x, y) -> mActivity.toggleUI());

        layout.addView(photoView, MATCH_PARENT, MATCH_PARENT);
        container.addView(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // 加载图片
        photoView.setLoading(true);
        // 缩略图, 必须和 smoothImageView 的图片一样的请求,才会使用同一个图片缓存
        RequestBuilder<Drawable> thumbnailRequest = mActivity.mGlideRequests
            .load(pictures.get(position).getFilePath())
            .apply(mActivity.mThumbnailRequestOptions);
        mActivity.mGlideRequests.load(pictures.get(position).getFilePath())
            .thumbnail(thumbnailRequest)
            // 必须设置占位图, 否则缩略图,未加载之前(imageView没有设置图片之前),无法拖动
            .placeholder(R.drawable.transparent_drawable)
            .error(R.drawable.ic_default_image_list)
            .override(3968, 3968)
            .apply(mRequestOptions)
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e,
                                            Object model,
                                            Target<Drawable> target,
                                            boolean isFirstResource) {
                    photoView.setLoading(false);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource,
                                               Object model,
                                               Target<Drawable> target,
                                               DataSource dataSource,
                                               boolean isFirstResource) {
                    photoView.setLoading(false);
                    return false;
                }
            })
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