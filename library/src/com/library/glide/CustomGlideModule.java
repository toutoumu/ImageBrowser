package com.library.glide;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ViewTarget;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.library.media.IPicture;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Objects;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

import static com.bumptech.glide.load.DataSource.LOCAL;

@GlideModule
public class CustomGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // 设置别的get/set tag id，以免占用View默认的
        // ViewTarget.setTagId(R.id.glide_tag_id);
        RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)//缓存策略
            // .format(DecodeFormat.PREFER_RGB_565)//图片格式
            //.placeholder(R.drawable.ic_default_image)//占位图
            .dontAnimate()
            .dontTransform();
        builder.setDefaultRequestOptions(options);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // 指定Model类型为Picture的处理方式
        registry.append(IPicture.class, InputStream.class, new MyModelLoader.LoaderFactory());

        // 指定Model类型为File的处理方式
    /*registry.append(File.class, InputStream.class,
        new FileLoader.Factory<InputStream>(new FileLoader.FileOpener<InputStream>() {

          @Override public InputStream open(File file) throws FileNotFoundException {
            // 可以在这里进行文件处理,比如解密等.
            Timber.e(file.getAbsolutePath());
            return ConcealUtil.getCipherInputStream(file);
          }

          @Override public void close(InputStream inputStream) throws IOException {
            inputStream.close();
          }

          @Override public Class<InputStream> getDataClass() {
            return InputStream.class;
          }
        }));*/
    }

    /**
     * 清单解析的开启
     *
     * 这里不开启，避免添加相同的modules两次
     *
     * @return {@link Boolean}
     */
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    public static class MyModelLoader implements ModelLoader<IPicture, InputStream> {

        public MyModelLoader() {
        }

        @Nullable
        @Override
        public LoadData<InputStream> buildLoadData(@NonNull IPicture model,
                                                   int width,
                                                   int height,
                                                   @NonNull Options options) {
            return new LoadData<>(new MyKey(model), new MyDataFetcher(model));
        }

        @Override
        public boolean handles(@NonNull IPicture s) {
            return true;
        }

        public static class LoaderFactory implements ModelLoaderFactory<IPicture, InputStream> {

            public LoaderFactory() {
            }

            @NonNull
            @Override
            public ModelLoader<IPicture, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
                return new MyModelLoader();
            }

            @Override
            public void teardown() {

            }
        }

        public static class MyKey implements Key {
            IPicture path;

            public MyKey(IPicture path) {
                this.path = path;
            }

            @Override
            public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
                // 缓存使用的Key,唯一标识文件对应的缓存
                messageDigest.update((path.getFilePath()).getBytes(CHARSET));
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                MyKey myKey = (MyKey) o;
                return Objects.equals(path, myKey.path);
            }

            @Override
            public int hashCode() {
                return path != null ? path.hashCode() : 0;
            }
        }

        public static class MyDataFetcher implements DataFetcher<InputStream> {

            private final IPicture file;
            private boolean isCanceled;
            InputStream mInputStream = null;

            public MyDataFetcher(IPicture file) {
                this.file = file;
            }

            @Override
            public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
                if (!isCanceled) {
                    try {
                        // long start = System.currentTimeMillis();
                        mInputStream = ConcealUtil.getCipherInputStream(file.getFilePath());
                        callback.onDataReady(mInputStream);
                        // Timber.e("耗时%s", System.currentTimeMillis() - start);
                    } catch (KeyChainException | CryptoInitializationException | IOException e) {
                        //Timber.e(e);
                        callback.onLoadFailed(e);
                    }
                }
                //callback.onDataReady(null);
            }

            @Override
            public void cleanup() {
                if (mInputStream != null) {
                    try {
                        mInputStream.close();
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                }
            }

            @Override
            public void cancel() {
                isCanceled = true;
            }

            @NonNull
            @Override
            public Class<InputStream> getDataClass() {
                return InputStream.class;
            }

            @NonNull
            @Override
            public DataSource getDataSource() {
                return LOCAL;
            }
        }
    }

  /*public static class FileOpen implements FileLoader.FileOpener<InputStream> {

    @Override
    public InputStream open(File file) throws FileNotFoundException {
      try {
        return ConcealUtil.getCipherInputStream(file);
      } catch (FileNotFoundException e) {
        throw e;
      } catch (IOException | KeyChainException | CryptoInitializationException e) {
        Timber.e(e);
        return null;
      }
    }

    @Override
    public void close(InputStream inputStream) throws IOException {
      inputStream.close();
    }

    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }
  }*/
}