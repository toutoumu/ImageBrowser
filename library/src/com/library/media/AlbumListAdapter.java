package com.library.media;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.library.glide.GlideApp;
import com.library.glide.GlideRequests;
import com.library.widget.ProgressImageView;
import java.util.List;
import org.wordpress.passcodelock.R;

public class AlbumListAdapter extends BaseAdapter {

    private final List<Album> mData;
    private final LayoutInflater mInflater;
    private final RequestOptions mRequestOptions;
    private final GlideRequests mGlideRequests;

    public AlbumListAdapter(Activity activity, List<Album> data) {
        mData = data;
        mInflater = activity.getLayoutInflater();
        mGlideRequests = GlideApp.with(activity);
        // mRequestOptions = RequestOptions.centerCropTransform().placeholder(R.drawable.ic_default_image);
        mRequestOptions = RequestOptions
            .fitCenterTransform()
            .override(500, 900)
            .error(R.drawable.ic_default_image_list)
            .placeholder(R.drawable.transparent_drawable)
            // .placeholder(R.drawable.ic_default_image)
            // https://www.jianshu.com/p/54bf089d0b04 解决Glide启用过渡时Placeholder变形问题
            // .placeholder(new GlidePlaceholderDrawable(activity.getResources(), R.drawable.ic_default_image))
            .dontAnimate()
            .dontTransform()
            .encodeQuality(80);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Album getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_album, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (holder != null) {
            Album album = getItem(position);
            holder.name.setText(album.getName());

            // Glide 4.x 加载数据
            mGlideRequests.load(album.getCover())
                .apply(mRequestOptions)
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
        }
        return convertView;
    }

    private static class ViewHolder {
        private final TextView name;
        private final ProgressImageView image;

        ViewHolder(View view) {
            name = view.findViewById(R.id.name);
            image = view.findViewById(R.id.image);
        }
    }
}
