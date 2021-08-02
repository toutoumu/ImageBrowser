package com.library.media;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.library.widget.DragFrameLayout;
import java.util.List;
import org.wordpress.passcodelock.R;

public class PictureAdapter2 extends RecyclerView.Adapter<PictureAdapter2.HorizontalVpViewHolder> {
    private final List<Picture> pictures;
    private final PictureActivity mActivity;
    private final RequestOptions mRequestOptions;

    public PictureAdapter2(PictureActivity activity, List<Picture> pictures) {
        this.mActivity = activity;
        this.pictures = pictures;

        // Glide 4.x 加载数据
        mRequestOptions = RequestOptions.fitCenterTransform()
            // .placeholder(R.drawable.transparent_drawable)
            // .placeholder(R.drawable.ic_default_image)
            // .placeholder(new GlidePlaceholderDrawable(mActivity.getResources(), R.drawable.ic_default_image))
            .override(3968, 3968);
    }

    @NonNull
    @Override
    public HorizontalVpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HorizontalVpViewHolder(LayoutInflater.from(mActivity).inflate((R.layout.item_photo), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HorizontalVpViewHolder holder, int position) {
        holder.mDragFrameLayout.setContainer(mActivity.bind.pager);
        holder.mDragFrameLayout.setImageView(mActivity.bind.smoothImageView);

        holder.mPhotoView.setMaximumScale(10);
        holder.mPhotoView.setMediumScale(4);
        holder.mPhotoView.setOnViewTapListener((view, x, y) -> mActivity.toggleUI());

        // 缩略图, 必须和 smoothImageView 的图片一样的请求,才会使用同一个图片缓存
        RequestBuilder<Drawable> thumbnailRequest = mActivity.mGlideRequests
            .load(pictures.get(position).getFilePath())
            .apply(mActivity.mThumbnailRequestOptions);

        // 加载图片
        holder.mPhotoView.setLoading(true);
        mActivity.mGlideRequests.load(pictures.get(position).getFilePath())
            .thumbnail(thumbnailRequest)
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
                    holder.mPhotoView.setLoading(false);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource,
                                               Object model,
                                               Target<Drawable> target,
                                               DataSource dataSource,
                                               boolean isFirstResource) {
                    holder.mPhotoView.setLoading(false);
                    return false;
                }
            })
            .into(holder.mPhotoView);
    }

    @Override
    public int getItemCount() {
        return pictures.size();
    }

    static class HorizontalVpViewHolder extends RecyclerView.ViewHolder {
        DragFrameLayout mDragFrameLayout;
        PhotoView mPhotoView;

        HorizontalVpViewHolder(@NonNull View itemView) {
            super(itemView);
            mDragFrameLayout = itemView.findViewById(R.id.drag_layout);
            mPhotoView = itemView.findViewById(R.id.photo_view);
        }
    }
}