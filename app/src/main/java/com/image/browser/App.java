package com.image.browser;

import android.app.Application;
import android.content.Context;
import com.blankj.utilcode.util.Utils;
import com.kongzue.dialog.util.DialogSettings;
import com.kongzue.dialog.util.TextInfo;
import com.kongzue.dialog.v3.Notification;
import com.image.browser.log.PrettyFormatStrategy;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

public class App extends Application {
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

        //Timber注册
        initLog();

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
        DialogSettings.checkRenderscriptSupport(context);
        DialogSettings.isUseBlur = true;
        DialogSettings.DEBUGMODE = true;
        DialogSettings.autoShowInputKeyboard = true;
        Notification.mode = Notification.Mode.FLOATING_WINDOW;
        // DialogSettings.backgroundColor = Color.BLUE;
        // DialogSettings.titleTextInfo = new TextInfo().setFontSize(50);
        // DialogSettings.buttonPositiveTextInfo = new TextInfo().setFontColor(Color.GREEN);
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

    public static Application getAppContext() {
        return sApplication;
    }
}
