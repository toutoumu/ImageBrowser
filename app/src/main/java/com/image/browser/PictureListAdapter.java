package com.image.browser;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.library.widget.ProgressImageView;
import java.util.List;

public class PictureListAdapter extends BaseAdapter {

    private final PictureActivity mActivity;
    private final LayoutInflater mInflater;
    private final List<Picture> mPictures;
    private boolean mSelect;

    public PictureListAdapter(PictureActivity activity, List<Picture> pictures, boolean select) {
        this.mActivity = activity;
        this.mPictures = pictures;
        this.mSelect = select;
        this.mInflater = activity.getLayoutInflater();
    }

    public void setSelect(boolean select) {
        this.mSelect = select;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mPictures.size();
    }

    @Override
    public Picture getItem(int i) {
        return mPictures.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_image, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Picture picture = getItem(position);
        if (holder != null && picture != null) {
            // 加载数据
            holder.image.setLoading(true);
            mActivity.mGlideRequests.load(picture.getFilePath())
                .apply(mActivity.mThumbnailRequestOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e,
                                                Object model,
                                                Target<Drawable> target,
                                                boolean isFirstResource) {
                        holder.image.setLoading(false);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource,
                                                   Object model,
                                                   Target<Drawable> target,
                                                   DataSource dataSource,
                                                   boolean isFirstResource) {
                        holder.image.setLoading(false);
                        return false;
                    }
                })
                .into(holder.image);

            if (mSelect) {//选择模式
                if (picture.isSelected()) {//选中
                    holder.mask.setVisibility(View.VISIBLE);
                    holder.checkMark.setVisibility(View.VISIBLE);
                    holder.checkMark.setImageResource(R.drawable.ic_checkbox_red_checked);
                } else {//未选中
                    holder.mask.setVisibility(View.GONE);
                    holder.checkMark.setVisibility(View.VISIBLE);
                    holder.checkMark.setImageResource(R.drawable.ic_checkbox_white_unchecked);
                }
            } else {//非选择模式
                holder.mask.setVisibility(View.GONE);
                holder.checkMark.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    private static class ViewHolder {
        View mask;
        ProgressImageView image;
        ImageView checkMark;

        ViewHolder(View view) {
            mask = view.findViewById(R.id.mask);
            image = view.findViewById(R.id.image);
            checkMark = view.findViewById(R.id.checkmark);
        }
    }
}
