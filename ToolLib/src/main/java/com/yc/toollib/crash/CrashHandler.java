package com.yc.toollib.crash;

import android.app.Application;
import android.content.Context;
import android.os.Looper;

import com.yc.toollib.tool.ToolLogUtils;

/**
 * <pre>
 *     @author yangchong
 *     email  : yangchong211@163.com
 *     time  : 2020/7/10
 *     desc  : 异常处理类
 *     revise:
 * </pre>
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    public static final String TAG = "CrashHandler";
    /**
     * 系统默认的UncaughtException处理类
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    /**
     * 程序的Context对象
     */
    private Context mContext;
    /**
     * CrashHandler实例
     */
    private static CrashHandler INSTANCE;
    /**
     * 监听
     */
    private CrashListener listener;


    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandler() {

    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (CrashHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CrashHandler();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化,注册Context对象,
     * 获取系统默认的UncaughtException处理器,
     * 设置该CrashHandler为程序的默认处理器
     *
     * @param ctx
     */
    public void init(Application ctx , CrashListener listener) {
        CrashToolUtils.init(ctx);
        CrashHelper.getInstance().install(ctx);
        mContext = ctx;
        this.listener = listener;
        //获取系统默认的UncaughtExceptionHandler
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //将当前实例设为系统默认的异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     * 该方法来实现对运行时线程进行异常处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        boolean isHandle = handleException(ex);
        initCustomBug(ex);
        if (mDefaultHandler != null && !isHandle) {
            //收集完信息后，交给系统自己处理崩溃
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            if (mContext instanceof Application){
                ToolLogUtils.w(TAG, "handleException--- ex----重启activity-");
                if (listener!=null){
                    listener.againStartApp();
                }
            }
        }
        CrashHelper.getInstance().setSafe(thread,ex);
    }

    /**
     * 初始化百度
     * @param ex
     */
    private void initCustomBug(Throwable ex) {
        //自定义上传crash，支持开发者上传自己捕获的crash数据
        //StatService.recordException(mContext, ex);
        if (listener!=null){
            //捕获监听中异常，防止外部开发者使用方代码抛出异常时导致的反复调用
            try {
                listener.recordException(ex);
            } catch (Throwable e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 自定义错误处理,收集错误信息
     * 发送错误报告等操作均在此完成.
     * 开发者可以根据自己的情况来自定义异常处理逻辑
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            ToolLogUtils.w(TAG, "handleException--- ex==null");
            return false;
        }
        //收集crash信息
        String msg = ex.getLocalizedMessage();
        if (msg == null) {
            return false;
        }
        ToolLogUtils.w(TAG, "handleException--- ex-----"+msg);
        ex.printStackTrace();
        //收集设备信息
        //保存错误报告文件
        CrashFileUtils.saveCrashInfoInFile(mContext,ex);
        return true;
    }


}
