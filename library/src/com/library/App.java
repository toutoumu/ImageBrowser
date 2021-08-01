package com.library;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import com.blankj.utilcode.util.Utils;
import com.facebook.soloader.SoLoader;
import com.kongzue.dialog.util.DialogSettings;
import com.kongzue.dialog.v3.Notification;
import com.library.log.PrettyFormatStrategy;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.BuildConfig;
import com.library.glide.ConcealUtil;
import timber.log.Timber;

public class App extends Application {
    // 默认为登录状态
    private static boolean isLogin = true;
    private static Application sApplication;

    @Override
    public void onCreate() {
        super.onCreate();

        sApplication = this;

        // 异常捕获
        MyCrashHandler handler = new MyCrashHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);

        // 工具方法类初始化
        Utils.init(this);

        // 加载conceal的so文件
        SoLoader.init(this, false);

        //Timber注册
        initLog();

        //初始化程序锁
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
        AppLockManager.getInstance().getAppLock().disable(); // 默认不启用锁
        AppLockManager.getInstance().getAppLock().setiUnlockLister((success, password) -> {
            if (success && !TextUtils.isEmpty(password)) {// 密码解锁
                // isLogin = ConcealUtil.init(App.this, password);
                isLogin = true;
            } else { // 指纹解锁
                isLogin = false;
                // ConcealUtil.destroy();
            }
        });

        ConcealUtil.init(this, "101010");

        // 对话框初始化
        initDialogs(this);
    }

    /**
     * 对话框初始化
     *
     * @param context
     */
    private static void initDialogs(Context context) {
        DialogSettings.init();
        // DialogSettings.checkRenderscriptSupport(context);
        // DialogSettings.isUseBlur = true;
        DialogSettings.DEBUGMODE = true;
        DialogSettings.autoShowInputKeyboard = true;
        Notification.mode = Notification.Mode.FLOATING_WINDOW;
        //DialogSettings.backgroundColor = Color.BLUE;
        //DialogSettings.titleTextInfo = new TextInfo().setFontSize(50);
        //DialogSettings.buttonPositiveTextInfo = new TextInfo().setFontColor(Color.GREEN);
        DialogSettings.style = DialogSettings.STYLE.STYLE_IOS;
        DialogSettings.theme = DialogSettings.THEME.LIGHT;
    }

    /**
     * 初始化日志打印
     */
    private void initLog() {
        // 日志格式化策略,使用自定义的策略
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()//
            .showThreadInfo(false)      // (Optional) Whether to show thread info or not. Default true
            .methodCount(1)             // (Optional) How many method line to show. Default 2
            .methodOffset(5)            // (Optional) Hides internal method calls up to offset. Default 5
            // .logStrategy(customLog)  // (Optional) Changes the log strategy to print out. Default LogCat
            .tag("")                    // (Optional) Global tag for every log. Default PRETTY_LOGGER
            .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));

        // Timber 使用Logger库打印日志
        Timber.plant(new Timber.DebugTree() {
            @Override
            protected void log(int priority, String tag, @NotNull String message, Throwable t) {
                Logger.log(priority, tag, message, t);
            }
        });
    }

    /**
     * 是否已经登录
     */
    public static boolean isLogin() {
        return isLogin;
    }

    /**
     * 设置是否已经登录
     */
    public static void setLogin(boolean login) {
        isLogin = login;
    }

    public static Application getAppContext() {
        return sApplication;
    }
}
